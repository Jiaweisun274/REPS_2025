# REPS_2025 – Renewable Energy Processing System

REPS_2025 is a Scala-based software system designed to facilitate the collection, analysis, visualization, and monitoring of renewable energy and electricity consumption data. The application supports both a command-line interface (CLI) and a graphical user interface (GUI), enabling flexible user interaction for data exploration and system operation.
## Group members
- Jiawei Sun, Catherine Barnes, Keshuo Wang
## System Features

- Energy data acquisition from Fingrid's public API, including solar, wind, hydroelectric production, and electricity consumption.
- Support for both CLI and GUI interaction modes.
- Data filtering by time intervals (hourly, daily, weekly, monthly).
- Descriptive statistics: mean, median, mode, range, and midrange calculations.
- Monitoring and alerting functionality for identifying low output or sudden drops.
- Visual representation of energy trends using line charts.
- Interactive data exploration via search and sorting tools.

## Software Requirements

To execute the application, the following dependencies are required:

- Java Development Kit (JDK) version 17 or higher.
- JavaFX SDK (e.g., version 21.0.7).
- sbt (Scala Build Tool) for project compilation and execution.
- Internet connectivity for accessing Fingrid’s API.

## API Key Usage

The system requires a valid Fingrid API key to access real-time data.

- In the CLI version, users are prompted to enter the API key at runtime.
- In the GUI, users may manually input the key into the provided field.

API keys can be obtained from the Fingrid Datahub: https://data.fingrid.fi/en/

## Running the Application

### Step 1: Clone the Repository

```bash
git clone https://github.com/Jiaweisun274/REPS_2025.git
cd REPS_2025
```
### Step 2: Enter sbt

```bash
sbt
```

### Step 3: Run the CLI Application
```bash
sbt runMain reps.ui.MenuApp
```

### Step 4: Run the GUI Application

```bash
sbt runMain reps.ui.MainGUI
```

When using IntelliJ IDEA, ensure that JavaFX runtime options are correctly configured:
```
--module-path /path/to/javafx-sdk/lib --add-modules javafx.controls,javafx.fxml
```

## Project Directory Structure

```
REPS_2025/
├── src/
│   ├── main/
│   │   └── scala/
│   │       └── reps/
│   │           ├── ui/          # User interfaces (CLI and GUI)
│   │           ├── analysis/    # Statistical analysis modules
│   │           ├── download/    # Fingrid data acquisition logic
│   │           ├── monitoring/  # Alerting and anomaly detection
│   │           ├── storage/     # File I/O and CSV parsing
│   │           └── models/      # Data model definitions
├── data/                         # Folder for downloaded CSV datasets
├── build.sbt                     # sbt configuration file
├── README.md                     # Project documentation
└── .gitignore                    # Git exclusion list
```

## Additional Information

- All downloaded datasets are stored in the `data/` directory.
- The GUI interface includes dynamic charts, tabular data browsing.

## License

This project is released under the MIT License. Copyright © 2025.
