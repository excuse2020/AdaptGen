import os

def get_avg_lines(path):
    total_lines = 0
    total_files = 0
    for root, dirs, files in os.walk(path):
        for file in files:
            file_path = os.path.join(root, file)
            with open(file_path, 'r') as file:
                lines = file.readlines()
                lines = [line for line in lines if line.strip() != '']
                total_lines += len(lines)
                total_files += 1

    print(path)
    print(total_lines)
    print(total_files)

    avg_lines = total_lines / total_files if total_files else 0

    return avg_lines, total_files


def save_avg_lines(avg_lines, result_path):
    if not os.path.exists(result_path):
        os.makedirs(result_path)
    with open(os.path.join(result_path, 'avg_lines'), 'w') as file:
        file.write(f"{avg_lines}\n")

def save_files(files, result_path):
    if not os.path.exists(result_path):
        os.makedirs(result_path)
    with open(os.path.join(result_path, 'files'), 'w') as file:
        file.write(f"{files}\n")

if __name__ == "__main__":
    root_folder = '/Users/zz/IdeaProjects/AdaptGen/datasets_lc/dataset5/'
    result_root = '/Users/zz/IdeaProjects/AdaptGen/result/'

    for m_folder in os.listdir(root_folder):
        m_path = os.path.join(root_folder, m_folder)
        if os.path.isdir(m_path):
            for n_folder in os.listdir(m_path):
                avg_lines, total_files = get_avg_lines(os.path.join(m_path, n_folder))
                result_path = os.path.join(result_root, m_folder, n_folder)
                save_avg_lines(avg_lines, result_path)
                save_files(total_files, result_path)
