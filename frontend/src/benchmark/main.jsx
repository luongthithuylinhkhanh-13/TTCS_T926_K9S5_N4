import React from 'react';
import { createRoot } from 'react-dom/client';
import BenchmarkApp from './BenchmarkApp';

const rootElement = document.getElementById('root');
if (rootElement) {
  // Mount directly without StrictMode to ensure exact, single-pass render profiling
  createRoot(rootElement).render(<BenchmarkApp />);
}
