/**
 * Utility functions for screening question answer options authoring.
 */

/**
 * Normalizes a label for duplicate comparison (trimmed, lowercased).
 *
 * @param {string} label
 * @returns {string}
 */
export function normalizeLabel(label) {
  return typeof label === 'string' ? label.trim().toLowerCase() : '';
}

/**
 * Calculates next display order as max(existing displayOrder) + 1, starting from 1.
 *
 * @param {Array<{displayOrder?: number|string}>} options
 * @returns {number}
 */
export function getNextDisplayOrder(options = []) {
  if (!Array.isArray(options) || options.length === 0) {
    return 1;
  }
  const orders = options
    .map((option) => Number(option?.displayOrder))
    .filter((order) => Number.isInteger(order));

  if (orders.length === 0) {
    return 1;
  }
  const maxOrder = Math.max(...orders);
  return maxOrder >= 1 ? maxOrder + 1 : 1;
}

/**
 * Auto-generates a valid, uppercase, snake_case option code from an option label.
 * Resolves collisions against existing option codes using a deterministic _2, _3 suffix.
 * Enforces a total maximum code length of 64 characters including any suffix.
 *
 * @param {string} label - Input option label
 * @param {string[]} [existingCodes=[]] - Existing codes for the question
 * @returns {string}
 */
export function generateOptionCode(label, existingCodes = []) {
  if (!label || typeof label !== 'string') {
    return 'OPTION';
  }

  // 1. Trim and uppercase
  let code = label.trim().toUpperCase();

  // 2. Replace whitespace and hyphens with underscores
  code = code.replace(/[\s-]+/g, '_');

  // 3. Strip all characters that are not alphanumeric or underscore
  code = code.replace(/[^A-Z0-9_]/g, '');

  // 4. Collapse repeated underscores
  code = code.replace(/_+/g, '_');

  // 5. Trim leading and trailing underscores
  code = code.replace(/^_+|_+$/g, '');

  // 6. Fallback if empty (e.g., label contained only special characters)
  if (!code) {
    code = 'OPTION';
  }

  // 7. Enforce initial 64 character ceiling
  if (code.length > 64) {
    code = code.substring(0, 64).replace(/_+$/, '');
  }

  const normalizedExisting = new Set(
    (existingCodes || []).map((c) => String(c).trim().toUpperCase())
  );

  if (!normalizedExisting.has(code)) {
    return code;
  }

  // 8. Handle collisions deterministically: append _2, _3, etc.
  const match = code.match(/^(.*)_(\d+)$/);
  const base = match ? match[1] : code;
  let counter = match ? parseInt(match[2], 10) + 1 : 2;

  while (true) {
    const suffix = `_${counter}`;
    const maxBaseLen = 64 - suffix.length;
    const safeBase = base.length > maxBaseLen
      ? base.substring(0, maxBaseLen).replace(/_+$/, '')
      : base;
    const candidate = `${safeBase}${suffix}`;
    if (!normalizedExisting.has(candidate)) {
      return candidate;
    }
    counter += 1;
  }
}

/**
 * Validates an option label for friendliness:
 * - Must not be blank
 * - Must not exceed 255 characters
 * - Must not be a case-insensitive duplicate of an existing option for the question
 *
 * @param {string} label
 * @param {Array<{id?: number|string, label: string}>} existingOptions
 * @param {number|string|null} [editingOptionId=null]
 * @returns {string|null} Friendly error message or null if valid
 */
export function validateOptionLabel(label, existingOptions = [], editingOptionId = null) {
  const trimmed = typeof label === 'string' ? label.trim() : '';
  if (!trimmed) {
    return 'Option label is required.';
  }
  if (trimmed.length > 255) {
    return 'Option label must not exceed 255 characters.';
  }

  const normalized = trimmed.toLowerCase();
  const isDuplicate = existingOptions.some((opt) => {
    if (editingOptionId && String(opt.id) === String(editingOptionId)) {
      return false;
    }
    return normalizeLabel(opt.label) === normalized;
  });

  if (isDuplicate) {
    return 'An option with this label already exists.';
  }

  return null;
}

/**
 * Parses multiline input for multiple options entry.
 * Validates lines against duplicates within the batch and against existing options.
 *
 * @param {string} text - Multiline string with one option per line
 * @param {Array<{code: string, label: string}>} existingOptions - Currently configured options
 * @returns {{ valid: boolean, error?: string, items?: Array<{code: string, label: string, typedValue: string, displayOrder: number}> }}
 */
export function parseMultipleOptions(text, existingOptions = []) {
  if (!text || typeof text !== 'string') {
    return { valid: false, error: 'Please enter at least one option label.' };
  }

  const rawLines = text.split(/\r?\n/).map((line) => line.trim()).filter(Boolean);
  if (rawLines.length === 0) {
    return { valid: false, error: 'Please enter at least one option label.' };
  }

  const existingNormalizedLabels = new Set(
    existingOptions.map((opt) => normalizeLabel(opt.label))
  );
  const currentCodes = existingOptions.map((opt) => opt.code);

  let nextOrder = getNextDisplayOrder(existingOptions);
  const batchLabels = new Set();
  const items = [];

  for (const label of rawLines) {
    if (label.length > 255) {
      return { valid: false, error: `Option "${label.substring(0, 30)}..." exceeds 255 characters.` };
    }

    const normalized = normalizeLabel(label);
    if (existingNormalizedLabels.has(normalized)) {
      return { valid: false, error: `An option with label "${label}" already exists.` };
    }
    if (batchLabels.has(normalized)) {
      return { valid: false, error: `Duplicate label "${label}" found in the entered options.` };
    }
    batchLabels.add(normalized);

    const code = generateOptionCode(label, currentCodes);
    currentCodes.push(code);

    items.push({
      code,
      label,
      typedValue: label,
      displayOrder: nextOrder,
    });
    nextOrder += 1;
  }

  return { valid: true, items };
}
