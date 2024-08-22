import os
import matplotlib.pyplot as plt
import seaborn as sns

# Define the root directory containing the results
root_dir = 'results/result_nk'
save_dir = 'img_res'

# Initialize a list to hold all the extracted floating-point numbers
fittest_values = []

# Traverse through each category directory
for problem in os.listdir(root_dir):
    problem_path = os.path.join(root_dir, problem)
    for category in os.listdir(problem_path):
        category_path = os.path.join(problem_path, category)
        
        # Check if it's a directory
        if os.path.isdir(category_path):
            # Path to the 'fittest' file
            fittest_file_path = os.path.join(category_path, 'fittest')
            
            # Check if the 'fittest' file exists
            if os.path.isfile(fittest_file_path):
                with open(fittest_file_path, 'r') as file:
                    lines = file.readlines()
                    if len(lines) >= 3:
                        # Extract the first floating-point number from the third line
                        third_line = lines[2].split()
                        if third_line:  # Ensure the line is not empty
                            try:
                                fittest_value = float(third_line[0])
                                fittest_values.append(fittest_value)
                            except ValueError:
                                print(f"Could not convert '{third_line[0]}' to float in file: {fittest_file_path}")
print(len(fittest_values))

# Plotting the boxplot with enhanced resolution and clarity
if fittest_values:
    plt.figure(figsize=(5, 4), dpi=200)  # Adjust figure size and resolution
    sns.boxplot(data=fittest_values, color='skyblue')
    
    # plt.title('Boxplot of Fittest Values (LeetCode)', fontsize=16, fontweight='bold')
    plt.ylabel('Fittest Values', fontsize=13)
    plt.xticks(fontsize=12)
    plt.yticks(fontsize=12)
    
    plt.grid(True, linestyle='--', alpha=0.6)  # Add grid for better readability
    
    # Save the plot to the img_res directory
    save_path = os.path.join(save_dir, 'fittest_boxplot_nk.png')
    plt.savefig(save_path, bbox_inches='tight')
    print(f"Boxplot saved to {save_path}")
else:
    print("No fittest values found.")