// Keep the existing ISO date format shared by both availability search forms.
export function getSearchDate(daysAhead = 0) {
  const date = new Date();
  date.setDate(date.getDate() + daysAhead);
  return date.toISOString().split('T')[0];
}
