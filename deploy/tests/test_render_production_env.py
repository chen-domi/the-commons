"""Verify optional NLP settings never enable inference by accident."""
import json
import os
from pathlib import Path
import stat
import subprocess
import tempfile
import unittest

AWS_STUB = r'''#!/usr/bin/env python3
import json, os, sys
args = sys.argv[1:]
flag = "--name" if "get-parameter" in args else "--names"
key = args[args.index(flag) + 1].rsplit("/", 1)[-1]
values = {"app-domain": "example.test", "database-username": "test",
          "database-password": "fake-db-secret", "google-client-id": "test-client",
          "google-client-secret": "fake-client-secret"}
values.update(json.loads(os.environ.get("TEST_NLP_PARAMETERS", "{}")))
print(values.get(key, "None"))
'''


class RenderProductionEnvTest(unittest.TestCase):
    def render(self, settings):
        with tempfile.TemporaryDirectory(prefix="commons-env-test-") as temporary:
            directory = Path(temporary)
            aws = directory / "aws"
            aws.write_text(AWS_STUB)
            aws.chmod(0o755)
            destination = directory / "test.env"
            environment = {**os.environ, "PATH": str(directory) + os.pathsep + os.environ["PATH"],
                           "TEST_NLP_PARAMETERS": json.dumps(settings)}
            script = Path(__file__).resolve().parents[1] / "render-production-env.sh"
            result = subprocess.run(["bash", str(script), str(destination)], env=environment,
                                    capture_output=True, text=True, timeout=20)
            self.assertEqual(result.returncode, 0, result.stderr)
            self.assertNotIn("fake-db-secret", result.stdout + result.stderr)
            self.assertNotIn("fake-client-secret", result.stdout + result.stderr)
            self.assertEqual(stat.S_IMODE(destination.stat().st_mode), 0o600)
            return destination.read_text()

    def test_missing_settings_keep_nlp_disabled(self):
        content = self.render({})
        self.assertIn("NLP_ENABLED=false\n", content)
        self.assertIn("BEDROCK_REGION=us-east-1\n", content)
        self.assertIn("BEDROCK_MODEL_ID=\n", content)

    def test_explicit_model_configuration_is_forwarded(self):
        content = self.render({"nlp-enabled": "true", "bedrock-region": "us-west-2", "bedrock-model-id": "test-model"})
        self.assertIn("NLP_ENABLED=true\n", content)
        self.assertIn("BEDROCK_REGION=us-west-2\n", content)
        self.assertIn("BEDROCK_MODEL_ID=test-model\n", content)


if __name__ == "__main__":
    unittest.main()
