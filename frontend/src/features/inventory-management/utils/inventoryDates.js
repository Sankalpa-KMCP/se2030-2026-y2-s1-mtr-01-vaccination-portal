
export const daysUntil = (date) => Math.ceil((new Date(`${date}T00:00:00`) - new Date()) / 86400000);
