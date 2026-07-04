## ADDED Requirements

### Requirement: Post-commit prediction display
After user commits text (上屏), the system SHALL display predicted next-word candidates in the candidate area based on the committed text.

#### Scenario: Prediction shown after commit
- **WHEN** user commits text (e.g., selects a candidate or confirms composition)
- **THEN** the candidate area SHALL show prediction candidates within 50ms
- **AND** prediction candidates SHALL be selectable like regular candidates

#### Scenario: No prediction candidates
- **WHEN** the prediction engine has no candidates for the current context
- **THEN** the candidate area SHALL be hidden/empty

### Requirement: Continuous prediction with iteration limit
After a prediction candidate is selected, the system SHALL automatically generate new predictions based on the accumulated text, up to a configurable limit.

#### Scenario: Continuous prediction cycle
- **WHEN** user selects a prediction candidate from the candidate area
- **THEN** the selected text SHALL be committed to the input field
- **AND** new prediction candidates SHALL appear based on the combined text

#### Scenario: Prediction iteration limit
- **WHEN** the prediction has reached `max_prediction` (e.g., 3) consecutive predictions
- **THEN** no further prediction candidates SHALL be generated
- **AND** the candidate area SHALL be hidden

### Requirement: Self-training via LevelDB
The prediction engine SHALL learn from user input automatically, storing n-gram associations in a LevelDB database without requiring pre-built data files.

#### Scenario: Record user input
- **WHEN** user commits text that is valid Chinese text (≤4 characters)
- **AND** the text does not match the previous commit (anti-backtracking)
- **THEN** the system SHALL store n-gram records (S, 2, 1, P) in the database
- **AND** SHALL associate it with the previous context

#### Scenario: Waterfall query
- **WHEN** querying predictions for a given context
- **THEN** the system SHALL first try S-Gram (sentence-level exact match)
- **AND** fall back to 2-Gram (bigram exact match) if no results
- **AND** fall back to 1-Gram (unigram match) if still no results
- **AND** finally try P-Gram (fuzzy suffix match) as last resort

### Requirement: Anti-interference mechanisms
The prediction system SHALL prevent invalid predictions through multiple safety mechanisms.

#### Scenario: ABA anti-backtracking
- **WHEN** the committed text is identical to the immediately previous commit
- **THEN** the system SHALL NOT record this as a new n-gram entry
- **AND** SHALL break the prediction chain

#### Scenario: Context timeout isolation
- **WHEN** the time between two commits exceeds `context_timeout` (default 5000ms)
- **THEN** the system SHALL reset the prediction history chain
- **AND** SHALL NOT use the previous context for prediction

#### Scenario: Punctuation sentence boundary
- **WHEN** user commits a terminal punctuation mark (. ! ? ， 。！？)
- **THEN** the system SHALL clear the context history
- **AND** start a fresh prediction chain

### Requirement: Prediction toggle
The system SHALL allow users to enable/disable prediction via a Rime option switch.

#### Scenario: Toggle via Rime option
- **WHEN** user sets Rime option `prediction` to `false`
- **THEN** the Lua processor SHALL stop monitoring commits
- **AND** the Lua translator SHALL stop generating prediction candidates
- **AND** the candidate area SHALL behave normally

#### Scenario: Toggle via UI key
- **WHEN** user presses a key configured as `{Option: prediction}` in the schema
- **THEN** the `prediction` Rime option SHALL be toggled
- **AND** the UI SHALL indicate the current prediction state

### Requirement: Lua component architecture
The prediction system SHALL be implemented as three separate Lua components: Processor, Translator, and Filter.

#### Scenario: Processor captures commit events
- **WHEN** user commits text
- **THEN** the Lua Processor SHALL receive the commit via `commit_notifier`
- **AND** update the in-memory history chain
- **AND** write n-gram data to LevelDB
- **AND** if prediction is enabled, inject placeholder input to trigger the Translator

#### Scenario: Translator generates prediction candidates
- **WHEN** the Processor has injected placeholder characters (›››) as input
- **THEN** the Lua Translator SHALL query the LevelDB for prediction candidates
- **AND** yield `Candidate` objects with type `"predict"`
- **AND** limit results to `max_candidates`

#### Scenario: Filter reorders candidates during input
- **WHEN** user is actively typing (during composition)
- **AND** `prediction` option is enabled
- **AND** `enable_context_reorder` is true
- **THEN** the Lua Filter SHALL move candidates that match predicted words to higher positions

### Requirement: Configuration via schema YAML
Users SHALL configure prediction behavior through standard Rime schema YAML configuration.

#### Scenario: Configure prediction in custom YAML
- **WHEN** user adds the three Lua components (processor, translator, filter) to their schema's engine configuration
- **AND** adds `user_predict` configuration block
- **AND** adds `prediction` switch
- **THEN** the system SHALL load these settings on deploy
- **AND** prediction SHALL function according to the configured parameters

#### Scenario: Configure prediction parameters
- **WHEN** user sets `user_predict/max_candidates` to `5`
- **AND** `user_predict/max_predictions` to `3`
- **AND** `user_predict/context_timeout` to `5000`
- **THEN** the system SHALL use these values for prediction behavior

### Requirement: Candidate area visibility after commit
The candidate area SHALL remain visible when prediction candidates are available, even when not in composing state.

#### Scenario: Candidate bar shows prediction
- **WHEN** text is committed (composing ends)
- **AND** prediction candidates are generated by the Lua Translator
- **THEN** the candidate bar SHALL remain visible
- **AND** display the prediction candidates

#### Scenario: Candidate bar hides when no prediction
- **WHEN** text is committed (composing ends)
- **AND** no prediction candidates are available
- **THEN** the candidate bar SHALL be hidden
