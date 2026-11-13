"""Offline tests: no GitHub/AWS writes, network access, or Docker required."""
import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest


STUB = r'''#!/usr/bin/env python3
import json, os, sys
from pathlib import Path
tool = Path(sys.argv[0]).name
args = sys.argv[1:]
case = os.environ.get("DEPLOY_TEST_CASE", "success")
with open(os.environ["DEPLOY_TEST_LOG"], "a") as log:
    log.write(json.dumps({"tool": tool, "args": args}) + "\n")
if tool == "gh":
    if any("/check-runs" in arg for arg in args):
        print(json.dumps([{"check_runs": [
            {"name": "test", "status": "completed", "conclusion": "success"},
            {"name": "build", "status": "completed", "conclusion": "failure" if case == "ci-failure" else "success"}
        ]}]))
    elif "--input" in args:
        json.load(sys.stdin)
        print("123")
    elif any("/statuses" in arg for arg in args):
        print("{}")
    else:
        print("a" * 40)
elif tool == "aws":
    if "get-caller-identity" in args:
        print("223455088185")
    elif "get-parameter" in args:
        print("example.test")
    elif "send-command" in args:
        print("test-command-id")
    elif "get-command-invocation" in args:
        print("Failed" if case == "ssm-failure" else "Success")
    else:
        sys.exit("Unexpected AWS call")
elif tool == "curl":
    print("502" if case == "api-failure" and args[-1].endswith("/api/auth/me")
          else "401" if args[-1].endswith("/api/auth/me") else "200")
elif tool != "sleep":
    sys.exit("Unexpected tool")
'''


class ProductionDeployTest(unittest.TestCase):
    def run_deploy(self, case):
        with tempfile.TemporaryDirectory(prefix="commons-deploy-test-") as temporary:
            directory = Path(temporary)
            log = directory / "calls.jsonl"
            for tool in ("gh", "aws", "curl", "sleep"):
                executable = directory / tool
                executable.write_text(STUB)
                executable.chmod(0o755)
            environment = {
                **os.environ,
                "PATH": str(directory) + os.pathsep + os.environ["PATH"],
                "DEPLOY_TEST_CASE": case,
                "DEPLOY_TEST_LOG": str(log),
                "GITHUB_REPOSITORY": "test/repo",
            }
            script = Path(__file__).resolve().parents[1] / "deploy-production.sh"
            result = subprocess.run(["bash", str(script)], env=environment,
                                    capture_output=True, text=True, timeout=20)
            calls = [json.loads(line) for line in log.read_text().splitlines()]
            return result, calls

    def states(self, calls):
        return [argument.removeprefix("state=") for call in calls
                if call["tool"] == "gh" for argument in call["args"]
                if argument.startswith("state=")]

    def test_success_requires_build_command_and_both_public_checks(self):
        result, calls = self.run_deploy("success")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(self.states(calls), ["in_progress", "success"])
        sent = next(call for call in calls if "send-command" in call["args"])
        commands = json.loads(sent["args"][sent["args"].index("--parameters") + 1])["commands"][0]
        self.assertIn("git checkout --detach " + "a" * 40, commands)
        self.assertIn("flock -n", commands)
        self.assertLess(commands.index("build backend"), commands.index("build frontend"))
        self.assertEqual(len([call for call in calls if call["tool"] == "curl"]), 2)

    def test_failed_ci_never_creates_a_deployment_or_sends_commands(self):
        result, calls = self.run_deploy("ci-failure")
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual(self.states(calls), [])
        self.assertFalse(any("send-command" in call["args"] for call in calls))

    def test_failed_ssm_command_cannot_report_success(self):
        result, calls = self.run_deploy("ssm-failure")
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual(self.states(calls), ["in_progress", "failure"])

    def test_working_homepage_with_broken_api_cannot_report_success(self):
        result, calls = self.run_deploy("api-failure")
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual(self.states(calls), ["in_progress", "failure"])


if __name__ == "__main__":
    unittest.main()
