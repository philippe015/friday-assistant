# 🤝 Contribuer à F.R.I.D.A.Y.

Merci de votre intérêt ! Ce dépôt suit un flux de développement classique.

## Mise en route

```bash
git clone https://github.com/philippe015/friday-assistant.git
cd friday-assistant
bash scripts/tester_sources.sh      # tests Java, Python, Rust et syntaxe JS
```

Pour compiler l'APK, voir **[BUILD.md](BUILD.md)** (JDK 17, Python 3.14, Rust, SDK 35, NDK).

## Flux de contribution

1. **Ouvrez une issue** (bug ou proposition) avant un changement important
2. **Forkez** puis créez une branche : `feat/ma-fonctionnalite`, `fix/mon-correctif`, `docs/...`
3. **Écrivez des tests** quand c'est possible (`tests/`, `native/src/lib.rs`)
4. Vérifiez localement : `bash scripts/tester_sources.sh`
5. **Ouvrez une Pull Request** vers `main` — la CI exécute les tests automatiquement
6. Décrivez *ce que* ça change et *pourquoi*

## Règles de code

| Langage | Conventions |
|---|---|
| Java | Java 17, package `fr.friday.assistant`, pas de dépendance non justifiée |
| Python | 3.14, module `friday_logic`, pas d'I/O réseau (le réseau vit côté Java) |
| Rust | `cargo fmt` + `cargo clippy` avant PR, code sans `unsafe` non justifié |
| HTML/JS | `index.html` : **un seul bloc `<script>`** (contrainte du test de syntaxe) |

## Sécurité et vie privée

- ❌ Ne jamais committer de **clé API**, keystore ou `local.properties`
- ✅ Les clés utilisateur passent par `SecurePrefs` (Android Keystore chiffré)
- ✅ Toute nouvelle permission Android doit être **justifiée** dans la PR
- ✅ Préserver le fonctionnement **hors-ligne** (heure/date via Python local)

## Processus de release

1. Mettre à jour `versionCode` / `versionName` dans `app/build.gradle`
2. Mettre à jour `CHANGELOG.md`
3. Commit, puis tag : `git tag -a vX.Y.Z -m "..." && git push origin vX.Y.Z`
4. La CI **compile, signe et publie** automatiquement l'APK dans la Release

> Les APK de release sont signés via des secrets GitHub (`KEYSTORE_*`). Seuls les mainteneurs peuvent publier.

## Signaler un problème

Utilisez les modèles d'issue : **Bug** ou **Proposition de fonctionnalité**.
Pour un problème de sécurité, évitez l'issue publique (contactez le mainteneur).
