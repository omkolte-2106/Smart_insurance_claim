import type { ReactNode } from "react";

type Props = {
  children: ReactNode;
  className?: string;
  elevated?: boolean;
};

export const Card: React.FC<Props> = ({ children, className = "", elevated }) => (
  <div className={`${elevated ? "si-card-elevated" : "si-card"} ${className}`}>{children}</div>
);
