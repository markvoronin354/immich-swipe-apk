# Agent Directives & Engineering Principles

Adhere strictly to Andrej Karpathy's operating principles across all changes:

## 1. Think Before Coding
- Clarify ambiguities, edge cases, and architectural assumptions upfront.
- Explicitly trace data flows, dependencies, and state boundaries before writing code.
- State the proposed solution and trade-offs before modifying core files.

## 2. Simplicity First
- Implement the simplest, most direct solution that satisfies requirements.
- Avoid premature abstractions, unnecessary interfaces, and unrequested boilerplate.
- Do not introduce libraries, patterns, or dead logic that were not requested.

## 3. Surgical Changes
- Touch only the lines and files strictly necessary to complete the task.
- Match existing formatting, patterns, and style conventions.
- Never refactor adjacent functions, reformat unchanged files, or adjust unrelated code.

## 4. Goal-Driven Execution
- Anchor every change to verifiable outcomes (compilation, unit tests, explicit logs).
- Confirm the build succeeds and existing behavior remains unbroken.
- If an issue or regression is introduced, revert cleanly to the working baseline instead of layering speculative patches.

## 5. 