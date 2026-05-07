## ADDED Requirements

### Requirement: Baidu engine selection option

The system SHALL support `"baidu"` as a valid value for the `recognition_service` preference key, which selects the `BaiduRecognizer` engine.

#### Scenario: Select Baidu engine via preference
- **WHEN** `recognition_service` preference is set to `"baidu"`
- **THEN** the `Speech` manager creates a `BaiduRecognizer` instance instead of `VivoRecognizer` or system `SpeechRecognizer`

#### Scenario: Baidu engine unavailable falls back gracefully
- **WHEN** `recognition_service` is set to `"baidu"` but `BaiduRecognizer` fails to initialize
- **THEN** an error is displayed to the user and no fallback engine is automatically selected (consistent with vivo behavior)

### Requirement: Baidu API credentials configuration

The system SHALL store and retrieve Baidu API Key and Secret Key from SharedPreferences for use by `BaiduRecognizer`.

#### Scenario: Read configured credentials
- **WHEN** `BaiduRecognizer` is initialized
- **THEN** it reads `baidu_api_key` and `baidu_secret_key` from SharedPreferences

#### Scenario: Missing credentials trigger error
- **WHEN** either `baidu_api_key` or `baidu_secret_key` is not set or is empty
- **THEN** `RecognizerListener.onError()` is called with a message indicating missing credentials
