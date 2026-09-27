# Work Conventions

## Main rules

- Do all work on your own branches cut from `dev`.
- One task == one branch.
- All changes must be merged via pull requests. `feature branch` -> `dev`, then `dev` -> `main`. Every pull request requires at least 1 approval before merging
- Delete merged feature branches after they have been successfully merged into `dev`.
- Before starting work ensure your local repo is up to date with `origin/dev`.
- Verify that the application builds and runs as intended before creating pull requests, do the same when reviewing pull requests.
- When in doubt, don't hesitate to contact other team members, communication is important.
### Main Branches

- `main` — production-ready most stable version
- `dev` — feature integration branch

### Branch Naming Convention

Use the following branch naming pattern:

```text
<type>/<short-description>
```

Examples:

- `docs/requirements-spec`
- `build/nextjs-init`
- `build/strapi-init`
- `feat/project-gallery`
- `feat/project-details-page`
- `fix/mobile-navigation`
- `test/filtering-regression`

### Allowed Prefixes

- `feat` — a new feature or functionality
- `fix` — a bug fix
- `docs` — changes to documentation
- `test` — test-related work
- `build` — project setup, build tools, or dependency changes
- `refactor` — code restructuring that neither fixes a bug nor adds a feature

## Commit Message Convention

Use clear and consistent commit messages based on conventional commit style. Keep commit messages imperative ("add", not "added").
```text
type: short description
```

Examples:

- `feat: add project gallery page`
- `fix: correct mobile menu behavior`
- `docs: update repository working conventions`
- `build: initialize Next.js frontend`


### Recommended PR Title Format
```text
type: short description
```

Examples:
- `feat: add project card component`
- `docs: add initial README and workflow rules`
- `build: setup Docker Compose for frontend and CMS`

## Testing

- New features should include appropriate tests. The person responsible for implementing a feature is also responsible for adding and maintaining tests for that feature.
- Existing tests must continue to pass before opening a pull request.