# AI-assisted inventory entry

## Behavior and boundaries

The Add Item form can send a description (up to 2,000 characters) to
`POST /api/inventory/parse-description`. The backend requires authentication,
CSRF protection, and management permission for the selected, registered
organization before attempting inference. E-board preview restrictions apply.

The Bedrock Converse request contains a fixed extraction prompt and the user's
description, not account information, database contents, or credentials. Users
are warned not to enter personal or confidential information. Descriptions and
raw model responses are not logged by this feature. Review AWS account-level
model invocation logging separately before collecting sensitive data.

Only name, category, location, integer quantity, last-used event and sharing
preference can be suggested. Unknown/missing values remain null. Unsupported
fields (including organization, roles, identifiers and dates), invalid types,
unknown categories, duplicate keys, trailing JSON, oversized responses and
truncated model output are rejected. These checks constrain structure, not
factual correctness; AI can still make mistakes, so users must review suggestions.

No database writes occur during inference. **Use suggestions** explicitly copies
non-null values into the editable form; **Add Item** still invokes the existing
inventory save/authorization flow. Manual entry remains available when AI is
disabled, unavailable, or returns an error. Editing existing items is unchanged.

## Defaults and cost controls

NLP is disabled by default, and no model is selected by default. Enabling the
feature makes descriptions available to the configured AWS model and can incur
charges. Check model availability, current pricing and IAM scope before enabling.
The code uses a 512-token output limit, one SDK attempt (no automatic retries),
a 15-second API deadline and 12-second attempt deadline. Credential resolution
and browser authentication are separate from the inference deadline.

There is one in-flight inference at a time, five requests per user per fixed
minute, and thirty requests globally per fixed minute. Provider failures also
consume quota. Limits are in-memory, reset on application restart, and apply to
one backend process only. They are safeguards, **not a hard AWS spending cap**.
Use shared rate limiting before scaling to multiple backend instances.

## Local setup (after choosing and authorizing a model)

The AWS SDK's default credential chain uses temporary credentials. For the
existing local profile, the SDK's `signin` module supports `aws login` sessions:

```bash
aws login --profile the-commons --region us-east-1
AWS_PROFILE=the-commons make dev
```

Set these non-secret options in the ignored `backend/.env` and restart:

```dotenv
NLP_ENABLED=true
BEDROCK_REGION=us-east-1
BEDROCK_MODEL_ID=YOUR_APPROVED_CONVERSE_MODEL_ID
```

Do not put access keys or tokens in the frontend or commit credentials. The
default `NLP_ENABLED=false` is sufficient to run all mocked tests without AWS.

## AWS deployment (not enabled by this PR)

1. Choose a model supported by Converse in the intended region and confirm pricing.
2. Grant the existing EC2 role `bedrock:InvokeModel` only for the chosen model ARN.
   Inference profiles may require extra profile/model ARNs; do not grant all models
   merely to work around a configuration error. No streaming action is used.
3. Verify that the backend container can retrieve role credentials using IMDSv2.
   AWS recommends metadata response hop limit 2 for containers. Keep IMDSv2 required;
   assess which containers can reach metadata before making this access change.
4. Set optional Parameter Store values under `/the-commons/production/`:
   `nlp-enabled`, `bedrock-region`, and `bedrock-model-id`. The production renderer
   defaults to disabled when these values are absent. No secret model key is needed.
5. After merge and green CI, deploy using `bash deploy/deploy-production.sh`.
6. Run a few consented live examples, confirm suggestions and saved organization
   ownership, then inspect usage. A mock passing does not prove model quality.

To disable, set `nlp-enabled` to `false` and redeploy. Manual item entry continues.
This change alone does not modify IAM, metadata settings, or cloud parameters.

## Checks

```bash
./backend/mvnw -f backend/pom.xml test '-Dtest=*Test,!InventoryRepositoryTest'
npm --prefix frontend test -- --watchAll=false --runInBand
```

Tests mock Bedrock and cover malformed JSON, field/quantity bounds, authorization
ordering, CSRF, limits, provider errors, partial drafts, and explicit review/save.
The PostgreSQL Testcontainers test additionally runs in backend CI. Manual live
testing remains required for extraction quality (clear descriptions, omissions,
multiple distinct items, and instruction-like text).

References: [Bedrock Converse](https://docs.aws.amazon.com/bedrock/latest/userguide/conversation-inference.html),
[Java local credentials](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials-temporary.html),
[EC2 metadata for containers](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/configuring-IMDS-new-instances.html).
