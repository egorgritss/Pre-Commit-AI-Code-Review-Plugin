# Pre-Commit AI Code Review Plugin

A proof of concept plugin that performs AI-powered code reviews before commits are submitted, helping to catch issues early in the development cycle.

---

## Overview

This Pre-Commit AI Code Review Plugin integrates advanced AI capabilities into your Git workflow, automatically reviewing selected code changes before they're committed. By detecting potential bugs, style issues, and suggesting improvements early in the development process, this tool helps maintain high code quality standards across your projects.

> [!NOTE]
> **Important Note:**  
> This plugin is a proof of concept and was developed in just two days without prior experience in plugin development.
> While functional, it lacks proper error handlings, configuration and UI parts. Requires refinements for production environments.

Watch the demonstration video: 
[![Watch the demonstration video](https://img.youtube.com/vi/GaJRW9n1KS4/maxresdefault.jpg)](https://youtu.be/GaJRW9n1KS4)
---

## Features

- **Automated AI code review** during the pre-commit phase
- **Selective code review** for specific files or changes
- **Immediate feedback** before committing code
- **Detection of common coding issues** and anti-patterns
- **Enforcement of coding standards** and best practices
- **Integration with existing Git workflows**
- **Context Collection:** Collects relevant context from your codebase, ensuring reviews are informed by surrounding code.
- **Retrieval-Augmented Generation (RAG):** Uses RAG techniques to enhance code review accuracy by retrieving and incorporating project-specific information (internal guidelines, coding conventions, similar code patterns) into the AI’s analysis and suggestions. This grounds the AI’s feedback in real examples from your repository, improving relevance and reducing false positives.

---

## Installation

1. **Clone this repository:**
    ```
    git clone https://github.com/egorgritss/Pre-Commit-AI-Code-Review-Plugin.git
    ```

2. **Navigate to the project directory:**
    ```
    cd Pre-Commit-AI-Code-Review-Plugin
    ```

3. **Run the plugin** (instructions may vary based on the build system) with:
   ```
   gradle runIde.
   ```
> [!NOTE]
> > Switch to new UI and rerun plugin of first run.   

---

## Configuration

To configure the plugin for your project, you need to specify the path to your coding guidelines.

> [!TIP]
> **Important:**  
> The `GUIDELINE_PATH` must be specified in the `AiReviewService` class to make static guidelines work. This path should point to a file containing your project’s coding standards and best practices that the AI will use during review.

---

## Usage

Once installed, the plugin automatically runs whenever you press magical button in the commit section. The AI reviewer will analyze only the selected changes that are staged for commit.

**To use the plugin:**

1. Make some changes
2. Select changes
3. Press magical button in commit section
4. The AI review will run automatically, analyzing your code
5. Review the feedback and make any necessary changes
6. Commit your code once all issues are resolved

---


