const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

export async function apiRequest(path, options = {}) {
  const config = {
    method: options.method || "GET",
    headers: {
      ...(options.body ? { "Content-Type": "application/json" } : {}),
      ...(options.headers || {}),
    },
    body: options.body ? JSON.stringify(options.body) : undefined,
  };

  const response = await fetch(`${API_BASE_URL}${path}`, config);

  if (options.responseType === "blob") {
    if (!response.ok) {
      const text = await response.text();
      let message = "Request failed";
      try {
        message = JSON.parse(text).message || message;
      } catch {
        message = text || message;
      }
      throw new Error(message);
    }
    return response.blob();
  }

  const text = await response.text();
  const data = text ? JSON.parse(text) : {};

  if (!response.ok) {
    throw new Error(data.message || "Request failed");
  }

  return data;
}

export { API_BASE_URL };
