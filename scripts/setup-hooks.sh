#!/usr/bin/env bash
#
# Устанавливает git-хуки проекта.
#
#     ./scripts/setup-hooks.sh
#
# Ставится pre-commit, который ищет секреты в том, что вы собираетесь
# закоммитить. История git публична навсегда: случайно попавший туда ключ
# придётся отзывать, а не удалять.

set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
hooks_dir="$(git -C "$repo_root" rev-parse --git-path hooks)"
mkdir -p "$hooks_dir"

hook="$hooks_dir/pre-commit"

cat > "$hook" <<'HOOK'
#!/usr/bin/env bash
set -euo pipefail

repo_root="$(git rev-parse --show-toplevel)"

# 1. Файлы, которые не должны попадать в репозиторий ни при каких условиях.
forbidden_pattern='(^|/)(secrets\.properties|keystore\.properties|local\.properties)$|\.(jks|keystore|p12|pem)$'
staged="$(git diff --cached --name-only --diff-filter=ACM)"
blocked="$(printf '%s\n' "$staged" | grep -E "$forbidden_pattern" || true)"
if [ -n "$blocked" ]; then
  echo "Коммит остановлен: эти файлы не должны попадать в репозиторий."
  printf '  %s\n' $blocked
  echo "Они перечислены в .gitignore. Уберите их: git restore --staged <файл>"
  exit 1
fi

# 2. Поиск секретов в содержимом.
if command -v gitleaks >/dev/null 2>&1; then
  if ! gitleaks protect --staged --redact --config "$repo_root/.gitleaks.toml"; then
    echo
    echo "Коммит остановлен: gitleaks нашёл похожее на секрет."
    echo "Если это ложное срабатывание — добавьте исключение в .gitleaks.toml"
    echo "и объясните его в описании pull request."
    exit 1
  fi
else
  echo "Предупреждение: gitleaks не установлен, проверка содержимого пропущена."
  echo "Установка: https://github.com/gitleaks/gitleaks#installing"
  echo "В CI эта проверка обязательна и всё равно выполнится."
fi
HOOK

chmod +x "$hook"
echo "Установлен pre-commit: $hook"

if ! command -v gitleaks >/dev/null 2>&1; then
  echo
  echo "gitleaks в системе не найден. Хук будет проверять только имена файлов."
  echo "Полная проверка содержимого: https://github.com/gitleaks/gitleaks#installing"
fi
