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
