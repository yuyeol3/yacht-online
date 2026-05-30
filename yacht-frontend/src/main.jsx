import "./polyfills.js";
import { createRoot } from "react-dom/client";
import App from "./App.jsx";
import "./layout.css";

const root = createRoot(document.getElementById("root"));
root.render(<App />);
