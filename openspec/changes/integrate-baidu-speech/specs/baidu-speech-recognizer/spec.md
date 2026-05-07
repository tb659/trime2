## ADDED Requirements

### Requirement: Baidu speech recognizer initialization

The system SHALL provide a `BaiduRecognizer` class that implements the `Recognizer` interface and initializes the Baidu Speech SDK with user-configured API credentials.

#### Scenario: Initialize with valid credentials
- **WHEN** `BaiduRecognizer` is constructed and valid API Key and Secret Key are available in SharedPreferences
- **THEN** the Baidu Speech SDK initializes successfully and is ready to accept recognition requests

#### Scenario: Initialize with missing credentials
- **WHEN** `BaiduRecognizer` is constructed but API Key or Secret Key is not configured
- **THEN** initialization fails gracefully and reports an error to the `RecognizerListener`

### Requirement: Start voice listening with Baidu engine

The system SHALL start a voice recognition session using the Baidu Speech SDK when `startListening()` is called, with VAD (Voice Activity Detection) parameters consistent with `VivoRecognizer`.

#### Scenario: Start listening with default VAD parameters
- **WHEN** `startListening()` is called on a `BaiduRecognizer` instance
- **THEN** the Baidu engine starts recording from the microphone with VAD enabled, front silence timeout of 5000ms, and end silence timeout of 1000ms

#### Scenario: Start inputting with shorter VAD parameters
- **WHEN** `startInputting()` is called on a `BaiduRecognizer` instance
- **THEN** the Baidu engine starts recording with a shorter end silence timeout (5000ms) and simple punctuation mode

### Requirement: Deliver recognition results through callback

The system SHALL deliver recognition results to the `RecognizerListener.onResult()` method when the Baidu engine returns a final recognition result.

#### Scenario: Successful recognition result
- **WHEN** the Baidu engine returns a final recognition result
- **THEN** `RecognizerListener.onEnd()` is called first, followed by `RecognizerListener.onResult(text)` on the main thread

#### Scenario: No speech detected
- **WHEN** the Baidu engine detects no speech within the timeout period
- **THEN** an appropriate error is reported via `RecognizerListener.onError()`

### Requirement: Stop and cancel recognition

The system SHALL provide `stop()` and `cancel()` methods that properly terminate the ongoing recognition session.

#### Scenario: Stop recognition normally
- **WHEN** `stop()` is called during an active recognition session
- **THEN** the Baidu engine stops recording and processes any buffered audio for final results

#### Scenario: Cancel recognition immediately
- **WHEN** `cancel()` is called during an active recognition session
- **THEN** the Baidu engine aborts the session immediately without returning results

### Requirement: Resource cleanup

The system SHALL release all Baidu Speech SDK resources when `destroy()` is called.

#### Scenario: Destroy recognizer
- **WHEN** `destroy()` is called on a `BaiduRecognizer` instance
- **THEN** the Baidu engine is stopped and all SDK resources are released

### Requirement: Language and user data configuration

The system SHALL support `setLanguage()` and `updateUserData()` methods for compatibility with the `Recognizer` interface.

#### Scenario: Set recognition language
- **WHEN** `setLanguage(language)` is called with a supported language code
- **THEN** the Baidu engine configures recognition for the specified language (zh_CN, zh_GD, en_GB)

#### Scenario: Update user data
- **WHEN** `updateUserData()` is called
- **THEN** any personalized lexicon or hot word data is uploaded to the Baidu engine (if supported)
