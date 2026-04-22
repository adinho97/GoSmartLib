export function inferNameParts(
  rawFirstName: string | null | undefined,
  rawLastName: string | null | undefined,
  candidates: Array<string | null | undefined>,
): { firstName: string; lastName: string } {
  let firstName = (rawFirstName || "").trim();
  let lastName = (rawLastName || "").trim();

  for (const candidate of candidates) {
    if (firstName && lastName) {
      break;
    }

    const value = (candidate || "").trim();
    if (!value) {
      continue;
    }

    const parts = value.split(/\s+/).filter(Boolean);
    if (parts.length < 2) {
      continue;
    }

    if (!firstName && lastName) {
      if (parts[0].toLowerCase() === lastName.toLowerCase()) {
        firstName = parts.slice(1).join(" ");
        continue;
      }
      if (parts[parts.length - 1].toLowerCase() === lastName.toLowerCase()) {
        firstName = parts.slice(0, -1).join(" ");
        continue;
      }
    }

    if (!lastName && firstName) {
      if (parts[0].toLowerCase() === firstName.toLowerCase()) {
        lastName = parts.slice(1).join(" ");
        continue;
      }
      if (parts[parts.length - 1].toLowerCase() === firstName.toLowerCase()) {
        lastName = parts.slice(0, -1).join(" ");
        continue;
      }
    }
  }

  return { firstName, lastName };
}

export function composeFullName(firstName: string, lastName: string): string {
  const normalizedFirstName = (firstName || "").trim();
  const normalizedLastName = (lastName || "").trim();
  return normalizedFirstName && normalizedLastName
    ? `${normalizedFirstName} ${normalizedLastName}`
    : "";
}

/**
 * Attempt to detect and fix a name that may be stored in reversed (lastname-first) format.
 * Compares the stored name against the current user's known name to determine correct order.
 * If the stored name matches "lastName firstName" pattern, reorders it to "firstName lastName".
 *
 * @param storedName The name as stored (potentially reversed from Smartschool)
 * @returns The name with corrected order, or original if order cannot be determined
 */
export function normalizeReviewAuthorName(storedName: string): string {
  const trimmed = (storedName || "").trim();
  if (!trimmed) {
    return trimmed;
  }

  const parts = trimmed.split(/\s+/).filter(Boolean);
  if (parts.length < 2) {
    return trimmed; // Single word, can't determine order
  }

  // Get current user's known name from localStorage
  const currentFirstName = (localStorage.getItem("firstName") || "")
    .trim()
    .toLowerCase();
  const currentLastName = (localStorage.getItem("lastName") || "")
    .trim()
    .toLowerCase();

  // Check if stored name matches "lastName firstName" pattern
  const firstName = parts[parts.length - 1].toLowerCase(); // Last part
  const lastName = parts.slice(0, -1).join(" ").toLowerCase(); // Everything except last part

  // If we recognize the current user's name in lastname-first order, reverse it
  if (
    currentFirstName &&
    currentLastName &&
    firstName === currentFirstName &&
    lastName === currentLastName
  ) {
    // Stored as "LastName FirstName", reorder to "FirstName LastName"
    const newFirst = parts[parts.length - 1];
    const newLast = parts.slice(0, -1).join(" ");
    return `${newFirst} ${newLast}`;
  }

  // Fallback: check if name looks like it's in lastname-first format
  // by seeing if first part is all caps or looks like a surname
  const firstPart = parts[0];
  if (
    firstPart.length >= 3 &&
    firstPart === firstPart.toUpperCase() &&
    parts[1] &&
    parts[1] !== parts[1].toUpperCase()
  ) {
    // Pattern like "PETERS Piet" - likely lastname first
    const reorderedParts = [parts[parts.length - 1], ...parts.slice(0, -1)];
    return reorderedParts.join(" ");
  }

  return trimmed; // Return original if we can't determine correct order
}
