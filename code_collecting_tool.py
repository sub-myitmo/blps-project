import sys
import os
from pathlib import Path


def process_directory(root_dir):
    root_path = Path(root_dir).resolve()
    output_lines = []
    counter = 0

    for file_path in root_path.rglob('*'):
        if file_path.is_file():
            try:
                relative_path = file_path.relative_to(root_path)
                content = file_path.read_text(encoding='utf-8', errors='ignore')

                output_lines.append(f"{relative_path}:")
                output_lines.append(content)
                output_lines.append("")
                counter += 1
            except Exception:
                continue

    output_file = root_path.name + "_contents.txt"
    with open(output_file, 'w', encoding='utf-8') as f:
        f.write('\n'.join(output_lines))

    print(f"Файл создан: {output_file}")
    print(f"Общее число прочитанных файлов: {counter}")


if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Использование: python script.py <путь_к_директории>")
        directory = input()
    else:
        directory = sys.argv[1]
    if not os.path.isdir(directory):
        print(f"Ошибка: '{directory}' не является директорией")
        sys.exit(1)

    process_directory(directory)