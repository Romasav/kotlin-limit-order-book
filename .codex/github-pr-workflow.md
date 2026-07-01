# GitHub PR and Merge Workflow

Use this when the user asks Codex to create a pull request and merge it into `main` for this repository.

## Expected Flow

1. Confirm the current branch and working tree:

   ```bash
   git status --short --branch
   git log -3 --oneline --decorate
   ```

2. Make sure the branch has already been committed and tests have passed.

3. Push the current feature branch:

   ```bash
   git push -u origin <branch-name>
   ```

4. Create the pull request with GitHub CLI:

   ```bash
   gh pr create \
     --repo Romasav/kotlin-limit-order-book \
     --base main \
     --head <branch-name> \
     --title "<title>" \
     --body "<body>"
   ```

5. Confirm the PR is mergeable:

   ```bash
   gh pr view <number> \
     --repo Romasav/kotlin-limit-order-book \
     --json number,state,mergeable,headRefName,headRefOid,baseRefName,url
   ```

6. Merge with a normal merge commit, matching the repository's existing history style:

   ```bash
   gh pr merge <number> \
     --repo Romasav/kotlin-limit-order-book \
     --merge \
     --match-head-commit <head-sha>
   ```

7. Sync local `main`:

   ```bash
   git fetch origin
   git switch main
   git pull --ff-only origin main
   ```

8. Verify the merged branch locally:

   ```bash
   ./gradlew test
   git status --short --branch
   git log -3 --oneline --decorate
   ```

## Notes From Phase 2

- The GitHub connector may fail with `403 Resource not accessible by integration`; use `gh` as the fallback.
- In the Codex sandbox, `gh` network commands may need escalated network access.
- Git metadata commands such as `git fetch`, `git switch`, and `git pull` may need escalation because they write under `.git`.
- If `gh auth status` says the local token is invalid, `gh` may still work when run with the approved network path; try the requested operation before asking the user to re-authenticate.
- Use `--match-head-commit` when merging so GitHub refuses the merge if the PR branch moved unexpectedly.
