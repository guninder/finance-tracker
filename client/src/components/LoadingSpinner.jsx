import { Loader2 } from 'lucide-react';

export default function LoadingSpinner({ fullPage = false, size = 32 }) {
  if (fullPage) {
    return (
      <div className="loading-spinner-fullpage">
        <Loader2 size={size} className="spinner-icon" />
      </div>
    );
  }

  return (
    <div className="loading-spinner">
      <Loader2 size={size} className="spinner-icon" />
    </div>
  );
}
