import { defineConfig, devices } from "@playwright/test";
export default defineConfig({
  testDir: "tests/browser",
  fullyParallel: false,
  use: { baseURL: "http://127.0.0.1:8791", trace: "retain-on-failure" },
  projects: [{ name: "chromium", use: { ...devices["Desktop Chrome"] } }],
  webServer: {
    command: "GOWORK=off DATA_DIR=/tmp/vervetwodo-pwa-browser-tests go run .",
    cwd: "server",
    url: "http://127.0.0.1:8791/healthz",
    reuseExistingServer: true,
    timeout: 120000,
  },
});
