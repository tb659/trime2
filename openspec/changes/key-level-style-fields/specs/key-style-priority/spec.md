## ADDED Requirements

### Requirement: Key-level style fields override row/keyboard/theme defaults

Each key in a keyboard Lua SHALL be able to set `text_color`, `text_size`, `background`, `padding`, and `margin` fields directly in its key table. These values SHALL have the highest priority in the style resolution chain.

#### Scenario: Key overrides text_color
- **WHEN** a key table in `rows[i].keys[j]` contains `text_color = 0xffff0000`
- **THEN** that key SHALL render its label text in color `0xffff0000`

#### Scenario: Key overrides text_size
- **WHEN** a key table in `rows[i].keys[j]` contains `text_size = 16`
- **THEN** that key SHALL render its label text at size 16sp

#### Scenario: Key overrides background
- **WHEN** a key table in `rows[i].keys[j]` contains `background = 0xff00ff00`
- **THEN** the key root background SHALL use `0xff00ff00`

#### Scenario: Key sets padding
- **WHEN** a key table in `rows[i].keys[j]` contains `padding = { left = 4, top = 2, right = 4, bottom = 2 }`
- **THEN** the keyRoot SHALL have the specified padding values

#### Scenario: Key sets margin
- **WHEN** a key table in `rows[i].keys[j]` contains `margin = { left = 1, top = 1, right = 1, bottom = 2 }`
- **THEN** the keyRoot SHALL have the specified margin values (mapped to `margins` subtable in style)

### Requirement: Row-level style fields override keyboard/theme defaults

Each row in a keyboard Lua SHALL be able to set `background`, `padding`, and `margin` fields directly in its row table. These values SHALL have priority over keyboard-level and theme-level defaults, but lower priority than key-level overrides.

Row-level `text_color` and `text_size` SHALL NOT be supported.

#### Scenario: Row background applies to keys without key-level background
- **WHEN** `rows[i]` contains `background = 0xffeeeeee`
- **AND** keys in that row do NOT have their own `background`
- **THEN** those keys SHALL use `0xffeeeeee` as their background color

#### Scenario: Row background is overridden by key background
- **WHEN** `rows[i]` contains `background = 0xffeeeeee`
- **AND** `rows[i].keys[j]` contains `background = 0xffffffff`
- **THEN** key j SHALL use `0xffffffff` (key-level wins)

#### Scenario: Row padding applies to keys without key-level padding
- **WHEN** `rows[i]` contains `padding = { left = 2, top = 2, right = 2, bottom = 2 }`
- **AND** keys in that row do NOT have their own `padding`
- **THEN** those keys' keyRoot SHALL have the specified padding

#### Scenario: Row margin applies to keys without key-level margin
- **WHEN** `rows[i]` contains `margin = { left = 1, top = 1, right = 1, bottom = 2 }`
- **AND** keys in that row do NOT have their own `margin`
- **THEN** those keys' keyRoot SHALL have the specified margin

### Requirement: Keyboard-level style fields override theme defaults

Keyboard Lua files SHALL support `key_text_color`, `key_text_size`, `key_background`, `key_padding`, and `key_margin` global variables as keyboard-level default style values. These SHALL have priority over theme `key` style defaults, but lower priority than row and key overrides.

#### Scenario: Keyboard text_color applies to keys without higher-priority override
- **WHEN** keyboard Lua has `key_text_color = 0xff333333`
- **AND** no row or key overrides `text_color`
- **THEN** all keys SHALL render label text in color `0xff333333`

#### Scenario: Keyboard text_color is overridden by row-level keys
- **WHEN** keyboard Lua has `key_text_color = 0xff333333`
- **AND** `rows[i]` has no background but a key in it has `text_color = 0xffff0000` (note: row-level text_color is not supported)
- **THEN** that specific key SHALL use `0xffff0000`

#### Scenario: Keyboard background is overridden by row background
- **WHEN** keyboard Lua has `key_background = 0xffcccccc`
- **AND** `rows[i]` has `background = 0xffeeeeee`
- **THEN** keys in row i SHALL use `0xffeeeeee`, keys in other rows SHALL use `0xffcccccc`

### Requirement: Theme key style is the lowest-priority fallback

If no key-level, row-level, or keyboard-level override is found for a style property, the theme's `key` style default SHALL be used.

#### Scenario: No overrides uses theme default
- **WHEN** a keyboard Lua has no `text_color` overrides at key, row, or keyboard level
- **THEN** keys SHALL use `key.text_color` from the active theme style

### Requirement: AbsKeyboardView supports keyboard-level defaults

`AbsKeyboardView` (keys-based layout) SHALL support keyboard-level `key_text_color`, `key_text_size`, `key_background`, `key_padding`, and `key_margin` defaults. Row-level overrides do not apply since `AbsKeyboardView` has no row concept.

#### Scenario: Keyboard text_color in keys-based layout
- **WHEN** a keyboard Lua with `keys = { ... }` has `key_text_color = 0xff333333`
- **AND** an individual key does NOT set `text_color`
- **THEN** that key SHALL use `0xff333333`

### Requirement: Backward compatibility

Existing keyboard Lua files without any of the new fields SHALL render identically to before this change.

#### Scenario: No new fields, no behavior change
- **WHEN** a keyboard Lua file has no `text_color`, `text_size`, `background`, `padding`, `margin` at key/row/keyboard level
- **THEN** all keys SHALL use the theme `key` style defaults as before
