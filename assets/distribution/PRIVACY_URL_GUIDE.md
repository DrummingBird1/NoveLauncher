# Privacy Policy URL — Hosting Guide

**✅ Done.** The policy is live at
**https://drummingbird1.github.io/NoveLauncher/privacy.html**, served by GitHub
Pages from this repository's [`docs/`](../../docs/) folder (terms of service:
[/terms.html](https://drummingbird1.github.io/NoveLauncher/terms.html)). Paste
that URL into Play Console / APKPure / any other store's privacy policy field.

To update it, edit `docs/privacy.html` and push — Pages redeploys automatically.
The canonical source text also lives beside this file as `PRIVACY_POLICY.md`
and `PRIVACY_POLICY.html`.

The rest of this document is kept as reference for the alternative hosting
options, in case the project ever moves off GitHub Pages.

Google Play **requires** a public HTTPS URL for your privacy policy.
A local Markdown file is not enough. Here are 4 free ways to host yours.

---

## Option 1: GitHub Pages (recommended — 5 minutes)

### Steps
1. Go to https://github.com/new and create a repo named `novelauncher-legal` (public)
2. Click **"creating a new file"**, name it `privacy.html`
3. Paste the HTML version of the privacy policy (see `PRIVACY_POLICY.html` in this folder)
4. Click **Settings → Pages → Source: main → Save**
5. Within ~1 minute your URL is live at:
   ```
   https://YOUR_USERNAME.github.io/novelauncher-legal/privacy.html
   ```

### To update later
Just edit `privacy.html` in the repo. Changes are live within seconds.

---

## Option 2: Cloudflare Pages (custom domain)

If you already own `novelauncher.app` (the email in our policy):

1. Go to https://dash.cloudflare.com/sign-up (free)
2. **Pages → Create project → Direct Upload**
3. Upload `PRIVACY_POLICY.html` and `TERMS_OF_SERVICE.html`
4. Custom domain → `novelauncher.app/privacy`

---

## Option 3: Notion (zero technical knowledge)

1. Open https://notion.so (free account)
2. Create a new page, paste the privacy policy content
3. **Share → Publish to web** → toggle on
4. Copy the public URL — paste it into Play Console

⚠️ Notion URLs change if you rename the page. Use a permanent option for production.

---

## Option 4: Google Sites (one-click)

1. Go to https://sites.google.com (free with Google account)
2. Create new site → paste content
3. **Publish** → custom URL e.g. `sites.google.com/view/novelauncher-privacy`

---

## What to put in Play Console

Play Console → **Policy → App content → Privacy policy** → paste your URL.

Example: `https://yourname.github.io/novelauncher-legal/privacy.html`

---

## ✅ Support email

The policy now uses `solvaris2@gmail.com` — a real, monitored inbox — so no further
action is needed before submission. If you later want a `@novelauncher.app` address
instead, buy the domain (~$30/year on Namecheap, Cloudflare Registrar) and replace
the email throughout the policy:
```bash
sed -i 's/solvaris2@gmail.com/your.new.email@novelauncher.app/g' PRIVACY_POLICY.md PRIVACY_POLICY.html TERMS_OF_SERVICE.md
```

Google **will** test the support email during review — keep it monitored.

---

## Pre-submission checklist

- [ ] Privacy policy hosted on HTTPS public URL
- [ ] Terms of service hosted on HTTPS public URL (optional but recommended)
- [ ] Email address in policy is real and monitored
- [ ] Both URLs accessible without login or paywall
- [ ] Both URLs return HTTP 200 (test with `curl -I YOUR_URL`)
- [ ] Same content visible on mobile (test with phone browser)
