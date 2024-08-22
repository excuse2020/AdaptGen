import matplotlib.pyplot as plt
import os

def _draw(dir):
    data_file = os.path.join(dir, "astringency")
    fittest_file = os.path.join(dir, "fittest")

    x = []
    y = []
    avg_total_score = None
    best_total_score = None

    with open(data_file, 'r') as file:
        for line in file:
            line_data = line.strip().split()
            x.append(float(line_data[0]))
            y.append(float(line_data[1]))

    with open(fittest_file, 'r') as file:
        lines = file.readlines()
        avg_total_score = float(lines[0].strip().split()[0])
        best_total_score = float(lines[1].strip().split()[0])

    # Set up figure size, font size, and DPI
    plt.figure(figsize=(7, 5), dpi=300)  # Reduce figure width, increase height slightly, and use high DPI
    plt.plot(x, y, label='Fitness')
    plt.axhline(y=avg_total_score, color='r', linestyle='--', label='Avg Total Score')
    plt.axhline(y=best_total_score, color='g', linestyle='--', label='Best Total Score')

    plt.title('Fitness Line Chart', fontsize=14)  # Increase font size for title
    plt.xlabel('Epoch', fontsize=12)  # Increase font size for x-axis label
    plt.ylabel('Fitness', fontsize=12)  # Increase font size for y-axis label

    # Move legend to the top, increase distance from title, and increase font size
    plt.legend(loc='upper center', bbox_to_anchor=(0.5, 1.25), ncol=3, fontsize=10)  # Increase the bbox_to_anchor y-value

    plt.tight_layout()

    # Save the figure with higher DPI for better clarity
    plt.savefig(os.path.join(dir, 'fitness.png'), dpi=300)
    plt.close()
    print(dir + ": Completed.\n")


def draw(dir):
    data_file = os.path.join(dir, "astringency")
    fittest_file = os.path.join(dir, "fittest")
    
    x = []
    y1 = []
    y2 = []
    y3 = []
    y4 = []
    y5 = []
    y6 = []
    y7 = []
    avg_total_score = None
    best_total_score = None
    
    with open(data_file, 'r') as file:
        for line in file:
            line_data = line.strip().split()
            x.append(float(line_data[0]))
            y1.append(float(line_data[1]))
            y2.append(float(line_data[2]))
            y3.append(float(line_data[3]))
            y4.append(float(line_data[4]))
            y5.append(float(line_data[5]))
            y6.append(float(line_data[6]))
            y7.append(float(line_data[7]))
    
    with open(fittest_file, 'r') as file:
        lines = file.readlines()
        avg_total_score = float(lines[0].strip().split()[0])
        best_total_score = float(lines[1].strip().split()[0])
    
    plt.figure()
    plt.plot(x, y1, label='Fitness')
    plt.plot(x, y2, label='freCbScore')
    plt.plot(x, y3, label='editDis')
    plt.plot(x, y4, label='csf')
    plt.plot(x, y5, label='pf')
    plt.plot(x, y6, label='validScore')
    plt.plot(x, y7, label='repetition')
    
    plt.axhline(y=avg_total_score, color='r', linestyle='--', label='Avg Total Score')
    plt.axhline(y=best_total_score, color='g', linestyle='--', label='Best Total Score')
    
    plt.title('Fitness Line Chart')
    plt.xlabel('Epoch')
    plt.ylabel('Score')
    plt.legend(loc='center left', bbox_to_anchor=(1, 0.5))
    plt.tight_layout()
    
    plt.savefig(os.path.join(dir, 'astringency.png'))
    plt.close()
    print(dir + ": Completed.\n")

for dir2_name in os.listdir("/Users/zz/IdeaProjects/AdaptGen/results/result_nk"):
    dir2 = os.path.join("/Users/zz/IdeaProjects/AdaptGen/results/result_nk", dir2_name)
    if os.path.isdir(dir2):
        for dir3_name in os.listdir(dir2):
            dir3 = os.path.join(dir2, dir3_name)
            if os.path.isdir(dir3):
                try:
                    _draw(dir3)
                    # draw(dir3)
                except Exception as e:
                    print(f"Exception details: {e}")
                finally:
                    continue

