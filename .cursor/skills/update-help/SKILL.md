---
name: update-help
description: >-
  Updates Avare in-app help (app/src/main/assets/help.html) to match current
  code, prepends an Avare Releases changelog entry whenever AndroidManifest
  versionName changes, and syncs Play store/whatsnew from that entry. Use when
  bumping versionCode/versionName, editing AndroidManifest.xml, writing release
  notes, updating help.html, or documenting user-facing Avare changes.
---

# Update Avare help.html

Keep `app/src/main/assets/help.html` accurate against the code, add a release note at the top of **Avare Releases** for every new `android:versionName` in `app/src/main/AndroidManifest.xml`, and sync Play “What’s new” from that same entry.

Do this in the same turn as a version bump. Do not wait to be asked.

## Workflow

```
- [ ] Read versionName from AndroidManifest.xml
- [ ] Read the top changelog version in help.html
- [ ] Find the last AndroidManifest version change in git (see below)
- [ ] Diff user-facing code since that commit
- [ ] Update help body sections that no longer match the app
- [ ] If versionName is new, prepend a changelog block from that diff
- [ ] If versionName already has an entry, update that entry's bullets
- [ ] Sync `store/whatsnew/whatsnew-en-US` from the latest Avare Releases entry
```

## Changes since last manifest version

Changelog bullets and help edits come from git history, not from memory or this chat.

1. Read current `android:versionName` from `app/src/main/AndroidManifest.xml`.
2. Read HEAD's versionName:

```bash
git show HEAD:app/src/main/AndroidManifest.xml | sed -n 's/.*android:versionName="\([^"]*\)".*/\1/p'
```

3. Find the commit that last **changed** the manifest version (newest first):

```bash
git log --format='%H' -G 'android:versionName="' -- app/src/main/AndroidManifest.xml
```

4. Choose the range start (`SINCE`):

| Working tree vs HEAD | `SINCE` |
|----------------------|---------|
| versionName **differs** from HEAD (bump not committed yet) | First hash from that log (last committed version change) |
| versionName **matches** HEAD (bump already committed) | **Second** hash from that log (the previous version change) |

5. Collect this release's changes (include uncommitted work):

```bash
git log --oneline ${SINCE}..HEAD
git diff ${SINCE} -- app/src/main
```

6. Write changelog and help only from user-facing app changes in that range (Activities, preferences, tabs, `*.html` assets, strings). Skip CI, Gradle, keystores, and Play upload.

If git history is missing or `SINCE` cannot be found, say so and stop rather than inventing bullets.

## Versions

Source of truth is the current `android:versionName` / `android:versionCode` in `app/src/main/AndroidManifest.xml`. Changelog headings use **versionName only** (e.g. `11.1.9`), never versionCode.

Find the current top entry: first `<p><b>X.Y.Z</b></p>` after `<h3 class="western">Avare Releases</h3>`.

| Situation | Action |
|-----------|--------|
| `versionName` is new (not the top help entry) | Insert a new changelog block immediately under `Avare Releases` |
| `versionName` already is the top entry | Update that entry's `<ul>` bullets; do not add a second block |
| Version was not bumped | Still fix help body if code/UI changed; do not invent a version |

If this session is bumping the version, add/update the changelog even if the user did not mention help.

## Changelog format

Insert **immediately after** `<h3 class="western">Avare Releases</h3>`. Copy this shape exactly (tabs, tags, period at end of each bullet):

```html
<h3 class="western">Avare Releases</h3>
<p><b>11.1.9</b></p>
<ul>
	<li>Fixed NOTAMs under Brief.</li>
</ul>
<p><b>11.1.8</b></p>
```

Real examples already in the file:

```html
<p><b>11.1.8</b></p>
<ul>
	<li>Fixed NOTAMs under Brief.</li>
</ul>
<p><b>11.1.6</b></p>
<ul>
	<li>Fix bottom Menu/Pan buttons and tab bar cut off under the system navigation bar on Android 15+.</li>
	<li>Bug fixes.</li>
	<li>Aircraft performance.</li>
</ul>
<p><b>11.1.3</b></p>
<ul>
	<li>Added Subscription.</li>
</ul>
```

Bullet rules:

- Short, user-facing. What a pilot sees, not class names or file paths.
- One idea per `<li>`. Start with a verb or concise phrase (`Fixed`, `Added`, `Bring back`, `Change`).
- End with a period.
- Use the app's own labels (`Brief`, `Preferences`, `Map`, `Plan`).
- Use `Bug fixes.` only for leftover non-user-visible fixes after the real bullets.
- Do not add dates, authors, issue numbers, or `versionCode`.
- Do not rewrite older entries.

## Play what’s new

After the latest **Avare Releases** `<ul>` is written or updated, regenerate Play notes:

```bash
bash .github/scripts/update-whatsnew.sh
```

That script copies the latest changelog bullets into `store/whatsnew/whatsnew-en-US` (Play’s 500-character limit). CI also runs it before a store upload. Do not hand-edit `whatsnew-en-US`; change the help changelog and run the script.

## Help body vs code

File: `app/src/main/assets/help.html`.

1. Collect user-facing changes from `git diff ${SINCE}` (last AndroidManifest version change) plus uncommitted `app/src/main` edits.
2. Find the help section that describes that feature (Quick Start, Tabs, Plan/Filing, Preferences paths, Extras).
3. Edit only what is wrong or missing. Keep the existing HTML 4.01 / Bootstrap voice, button names, and `&nbsp;` / `<B>` style.
4. Do not rewrite donations, Notice, or FAA caution text unless the described procedure changed.
5. Do not invent features. If help mentions a button/path, it must exist in code (`res/xml/preferences.xml`, tab layouts, `*Activity.java`, `*Interface.java`, HTML assets like `plan.html`).

Areas that drift often:

- Plan Brief / NOTAMs
- Subscription / Pro / register / 1800wxbrief
- Preferences paths and labels
- Map Menu, tabs, and Android 15+ system-bar behavior
- Download / chart limits for free vs subscribed users

## Do not

- Change `versionName` / `versionCode` from this skill unless the user asked for a version bump.
- Reformat the whole `help.html`.
- Add a changelog when versionName is unchanged.
- Hand-write `store/whatsnew/whatsnew-en-US`; always generate it from help.html.
