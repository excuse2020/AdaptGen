## AdaptGen

AdaptGen is a problem-adaptive solution template generation method for online programming platforms. It analyzes and extracts problem-solving patterns from various programming problems' solutions and generates solution templates tailored to each problem. The templates contain only the basic algorithm structure and necessary boilerplate code, leaving the core logic for programmers to complete.

### Key Features

- **Genetic Programming-Based Solution Template Generation**: Utilizes genetic programming to generate solution templates for coding tasks.
- **Efficient Encoding Strategy**: A linear hashing sequence encoding strategy is used for solution representation.
- **Evolutionary Operators**: Selection and crossover operators with de-duplication, random crossover, and stratified selection to maintain diversity.
- **Custom Fitness Function**: Directs the evolutionary process towards effective solution templates.
- **Semantic Abstraction and Core Code Concealment**: After the genetic algorithm evolves an optimal solution, it is decoded from an encoded hash sequence into a final solution template. This involves converting the hash sequence into source code and abstract expressions, categorizing code statements to distinguish core and non-core parts, concealing core parts, and transforming non-core parts into template elements. This process covers various code structures like loops, conditionals, declarations, and method calls.

### Datasets

Two datasets were constructed from popular platforms:

- **LeetCode**: 841 programming tasks with a total of 2,730 solutions.
- **NowCoder**: 156 programming tasks with a total of 489 solutions.

### Evaluation Results

- **Template Generation Qualification Rate**: 77%-85%
- **Applicability Rate**: 80%
- **Coding Efficiency Improvement**: 60% (based on edit distance)

### Project Components

- **`datasets/`**: Contains datasets from LeetCode and NowCoder for evaluation. 
- **`py/`**: Python scripts for analyzing the results of RQ1 (Research Questions).
- **`src/main/java/evalute/`**: Java classes for evaluating AdaptGen's performance on different research questions.
- **`src/main/java/realize/`**: Implementation of core components such as encoding, fitness calculation, genetic algorithm (GA) operations, preprocess, and utility functions.
- **`pom.xml`**: Maven configuration file for managing dependencies and building the project.

## Setup

### Prerequisites

- **Java 17**: Ensure you have Java installed on your system.
- **Maven**: For building and managing project dependencies.
- **Python 3.x**: For running Python scripts for result analysis.
- **Python Libraries**: Install necessary libraries.
- **IDE:** IntelliJ IDEA, Eclipse, or any other IDE that supports Maven projects.

### Environment Setup

To set up the project in IntelliJ IDEA and manage dependencies with Maven:

1. **Open IntelliJ IDEA**:
   - Start IntelliJ IDEA and select **"Open"** from the Welcome screen or **"File > Open..."** if you already have another project open.

2. **Import the Project**:
   - Navigate to the root directory of the `AdaptGen` project that you cloned.
   - Select the `pom.xml` file and click **"Open"**.
   - When prompted, select **"Open as Project"**.

3. **Import Project from Maven**:
   - If IntelliJ IDEA detects a Maven project, it will automatically configure it for you. 
   - If not, go to **"File > Project Structure > Project"**, and set the **Project SDK** to the appropriate version of Java.

4. **Download Dependencies**:
   - IntelliJ IDEA should automatically start downloading the dependencies defined in the `pom.xml` file. If it doesn't, you can manually trigger it:
     - Open the **"Maven"** tool window from the right sidebar.
     - Click on the **"Reload All Maven Projects"** button (two arrows forming a circle).

5. **Build the Project**:
   - Once the dependencies are downloaded, you can build the project by clicking **"Build > Build Project"** from the top menu or using the shortcut **`Ctrl+F9`**.

6. **Run the Main Class**:
   - Locate the main class under `src/main/java/evalute/Main.java` or any other main class you want to execute.
   - Right-click on the file and select **"Run 'Main'"** to run the project.

7. **Verify the Setup**:
   - Ensure that all dependencies are resolved without errors, and the project builds successfully.

### How to Use AdaptGen

1. **Prepare the Dataset**: Ensure you have the datasets ready in the `datasets/` directory.

2. **Generate Solution Templates**:  
   If you want to generate solution templates for programming problems, place the Accepted (AC) code corresponding to the programming problem in the `datasets` folder following the format of the existing files. Alternatively, you can write your own code to call the `start` function in `GAStart` to generate templates programmatically.

3. **Evaluate and Refine**: Use the provided scripts and classes to evaluate the quality of the generated templates and refine the parameters for better results.

## Acknowledgements

- **LeetCode and NowCoder**: For providing the programming tasks and solution data.

## Contact

For questions, please contact the project maintainers at [474839426@qq.com].
