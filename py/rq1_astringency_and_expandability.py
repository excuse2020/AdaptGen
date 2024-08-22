import os
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd

dataset = "lc"

def get_convergence_iterations(result_folder):
    convergence_iterations = []
    non_convergence = []
    # 遍历result文件夹下的所有文件夹
    for root, dirs, files in os.walk(result_folder):
        for dir_name in dirs:
            subdir_path = os.path.join(root, dir_name)
            
            # 遍历更深一级的文件夹
            for subroot, subdirs, subfiles in os.walk(subdir_path):
                for subdir_name in subdirs:
                    subsubdir_path = os.path.join(subroot, subdir_name)
                    
                    # 查找astringency文件
                    astringency_file_path = os.path.join(subsubdir_path, 'astringency')
                    fittest_file_path = os.path.join(subsubdir_path, 'fittest')
                    non_empty_lines = None
                    with open(fittest_file_path, 'r') as f:
                        lines = f.readlines()
                        non_empty_lines = [line for line in lines if line.strip() != '']
                        
                    if os.path.isfile(astringency_file_path):
                        # 计算行数
                        with open(astringency_file_path, 'r') as file:
                            epoch = len(file.readlines())
                            if len(non_empty_lines) < 3:
                                non_convergence.append(epoch)
                            else:
                                convergence_iterations.append(epoch - 500)
                    else:
                        print(astringency_file_path)
    return convergence_iterations, non_convergence

def plot_histogram(data, img_dir, name):
    """
    绘制收敛轮次分布的直方图并保存到img目录
    """
    plt.figure(figsize=(9, 5), dpi=200)  # 增大图片尺寸和分辨率
    plt.hist(data, bins=20, edgecolor='black', alpha=0.7)
    plt.title('Distribution of Convergence Iterations', fontsize=18)  # 增加标题字体大小
    plt.xlabel('Convergence Iterations', fontsize=14)
    plt.ylabel('Frequency', fontsize=14)
    plt.xticks(fontsize=12)  # 增大X轴刻度字体大小
    plt.yticks(fontsize=12)  # 增大Y轴刻度字体大小
    plt.grid(True)
    if not os.path.exists(img_dir):
        os.makedirs(img_dir)
    plt.savefig(os.path.join(img_dir, name), dpi=300)  # 增加保存时的分辨率
    plt.close()

def plot_boxplot(data, img_dir, name):
    """
    绘制收敛轮次分布的箱线图并保存到img目录
    """
    plt.figure(figsize=(10, 6))
    plt.boxplot(data, vert=False, patch_artist=True)
    plt.title('Boxplot of Convergence Iterations')
    plt.xlabel('Convergence Iterations')
    plt.grid(True)
    if not os.path.exists(img_dir):
        os.makedirs(img_dir)
    plt.savefig(os.path.join(img_dir, name))
    plt.close()

def process_fittest_files(result_folder):
    more = []
    less = []
    
    for root, dirs, files in os.walk(result_folder):
        for dir_name in dirs:
            subdir_path = os.path.join(root, dir_name)
            
            for subroot, subdirs, subfiles in os.walk(subdir_path):
                for subdir_name in subdirs:
                    subsubdir_path = os.path.join(subroot, subdir_name)
                    
                    fittest_file_path = os.path.join(subsubdir_path, 'fittest')
                    if os.path.isfile(fittest_file_path):
                        with open(fittest_file_path, 'r') as file:
                            lines = file.readlines()
                            lines = [line for line in lines if line.strip() != '']
                            if len(lines) >= 3:
                                try: 
                                    avg_total_score = float(lines[0].strip().split()[0])
                                    best_generated_score = float(lines[2].strip().split()[0])
                                    if best_generated_score >= avg_total_score:
                                        more.append(fittest_file_path)
                                    else:
                                        less.append(fittest_file_path)
                                except Exception as e:
                                    print(fittest_file_path)
    return more, less

def rq11():
    result_folder = '/Users/zz/IdeaProjects/AdaptGen/results/result_' + dataset
    img_dir = 'img_res'
    convergence_iterations, non_convergence = get_convergence_iterations(result_folder)

    print(convergence_iterations)
    print("-" * 20)
    print(len(convergence_iterations))
    print("-" * 20)
    average = sum(convergence_iterations) / len(convergence_iterations)
    print(f"平均数: {average}")
    print("-" * 20)
    print(non_convergence)
    print("-" * 20)
    print(len(non_convergence))

    c = [i for i in convergence_iterations if i <= 2000]
    print(len(c) / len(convergence_iterations))

    # 绘制并保存直方图
    plot_histogram(convergence_iterations, img_dir, 'convergence_iterations_histogram_' + dataset + '.png')

    # 绘制并保存箱线图
    # plot_boxplot(convergence_iterations, img_dir, 'convergence_iterations_boxplot.png')

    # convergence_iterations = [i for i in convergence_iterations if i <= 10000]
    # plot_histogram(convergence_iterations, img_dir, 'convergence_iterations_histogram_filter_' + dataset + '.png')
    # plot_boxplot(convergence_iterations, img_dir, 'convergence_iterations_boxplot_filter.png')


def rq12():
    result_folder = '/Users/zz/IdeaProjects/AdaptGen/result'
    more, less = process_fittest_files(result_folder)
    m = len(more)
    n = len(less)
    print(more[0])
    print(m)
    print(n)
    print(m / 2730) # 0.8503118503118503

def less2000():
    result_folder = '/Users/zz/IdeaProjects/AdaptGen/result'
    convergence_iterations = get_convergence_iterations(result_folder)
    convergence_iterations = [i for i in convergence_iterations if i <= 10000]
    m = len(convergence_iterations)
    convergence_iterations = [i for i in convergence_iterations if i <= 2000]
    n = len(convergence_iterations)
    print(n / m)


def rq13(attr):
    # 设置路径
    folder_path = 'result'

    # 初始化数据存储
    avg_lines = []
    avg_fitness = []
    evolved_results = []
    epochs = []

    max_lines = 0

    # 遍历文件夹
    for root, dirs, files in os.walk(folder_path):
        for dir in dirs:
            # 找到更深一层的文件夹
            subdir_path = os.path.join(root, dir)
            for sub_root, sub_dirs, sub_files in os.walk(subdir_path):
                for sub_dir in sub_dirs:
                    # 找到更深一层的文件夹
                    sub_subdir_path = os.path.join(sub_root, sub_dir)

                    fittest_file_path = os.path.join(sub_subdir_path, 'fittest')
                    if os.path.isfile(fittest_file_path):
                        with open(fittest_file_path, 'r') as file:
                            lines = file.readlines()
                            non_empty_lines = [line for line in lines if line.strip() != '']
                            if len(non_empty_lines) < 3:
                                continue
                    
                    for _, _, files in os.walk(sub_subdir_path):
                        for file in files:
                            try:
                                file_path = os.path.join(sub_subdir_path, file)
                                if file == attr:
                                    with open(file_path, 'r') as f:
                                        f = float(f.read().strip())
                                        avg_lines.append(f)
                                        max_lines = max(max_lines, f)
                                elif file == 'fittest':
                                    with open(file_path, 'r') as f:
                                        lines = f.readlines()
                                        avg_fitness.append(float(lines[0].strip().split()[0]))
                                        evolved_results.append(float(lines[2].strip().split()[0]))
                                elif file == 'astringency':
                                    with open(file_path, 'r') as f:
                                        epochs.append(int(len(f.readlines())))
                            except Exception as e:
                                print(file_path)

    print(max_lines)
    print(sum(avg_lines) / len(avg_lines))
    # 定义代码长度区间
    length_bins = np.arange(0, max_lines + 5, 5)
    length_bin_labels = [f'{i}-{i+5}' for i in length_bins[:-1]]

    # 分配代码长度到区间
    length_bin_indices = np.digitize(avg_lines, bins=length_bins)

    # 统计每个区间的代码个数、演化结果超过平均结果的比例和平均epoch
    bin_code_counts = []
    bin_proportions = []
    bin_avg_epochs = []

    for i in range(1, len(length_bins)):
        bin_indices = length_bin_indices == i
        if np.sum(bin_indices) > 0:
            bin_code_counts.append(np.sum(bin_indices))
            bin_proportions.append(np.mean(np.array(evolved_results)[bin_indices] > np.array(avg_fitness)[bin_indices]))
            bin_avg_epochs.append(np.mean(np.array(epochs)[bin_indices]))
        else:
            bin_code_counts.append(0)
            bin_proportions.append(0)
            bin_avg_epochs.append(0)

    # 创建表格
    data = {
        'Length Range': length_bin_labels,
        'Code Count': bin_code_counts,
        'Proportion Above Average': bin_proportions,
        'Average Epoch': bin_avg_epochs
    }
    df = pd.DataFrame(data)

    excel_file_path = attr + "_evolution_results_table_lc.xlsx"
    # 保存表格到Excel文件
    df.to_excel(excel_file_path, index=False)


if __name__ == "__main__":
    # rq12()
    # rq13('files')
    # rq13('avg_lines')
    # rq13()
    rq11()
