# Project conventions

- The Maven coursework lives in `tp1-aeds3`; use the supplied `aed3` file and index implementations rather than replacing them, to preserve the discipline's structures.
- Keep text normalization and keyword scoring in dedicated persistence helpers; `BancoDados` coordinates record/index mutations and `Menu` only presents ordered results.
- The inverted-index document count represents active questions, including questions with no valid keyword tokens, so TF×IDF uses the same searchable corpus as the menu.
- Preserve professor source attribution; additive file-close support in the Maven copy of `ListaInvertida` manages its file lifecycle without changing its storage algorithms.
- The preview uses the package `dev` script to serve a read-only README view; it does not replace the Java console application or expose binary data files.