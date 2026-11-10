import type { ReactNode } from "react";

// Paths from the Android task controls in app/src/main/res/drawable.
const paths = {
  repeat: "M7,7h10v3l4,-4 -4,-4v3H5v6h2zM17,17H7v-3l-4,4 4,4v-3h12v-6h-2z",
  rollover:
    "M8,3v4M16,3v4M4,10h16M10,21H5a2,2 0,0 1,-2,-2V7a2,2 0,0 1,2,-2h14a2,2 0,0 1,2,2v5M13,18h8M18,15l3,3 -3,3",
  agenda:
    "M19,4h-1V2h-2v2H8V2H6v2H5c-1.1,0 -2,0.9 -2,2v14c0,1.1 0.9,2 2,2h14c1.1,0 2,-0.9 2,-2V6c0,-1.1 -0.9,-2 -2,-2zM19,20H5V10h14v10zM19,8H5V6h14v2zM7,12h4v4H7z",
  clock:
    "M12,2a10,10 0,1 0,0 20a10,10 0,0 0,0 -20M12,20a8,8 0,1 1,0 -16a8,8 0,0 1,0 16M12.5,7H11v6l5.25,3.15 0.75,-1.23 -4.5,-2.67Z",
  tags: "M21.4,11.6l-9,-9C12,2.2 11.5,2 11,2H4C2.9,2 2,2.9 2,4v7c0,0.5 0.2,1 0.6,1.4l9,9c0.8,0.8 2,0.8 2.8,0l7,-7c0.8,-0.8 0.8,-2 0,-2.8zM13,20L4,11V4h7l9,9 -7,7zM6.5,5A1.5,1.5 0,1 0,6.5 8A1.5,1.5 0,1 0,6.5 5",
  checklist:
    "M221,647L363,505Q375,493 391,493.5Q407,494 419,506Q430,518 430,534Q430,550 419,562L250,732Q238,744 222,744Q206,744 194,732L108,646Q97,635 97,618Q97,601 108,590Q119,579 136,579Q153,579 164,590L221,647ZM221,327L363,185Q375,173 391,173.5Q407,174 419,186Q430,198 430,214Q430,230 419,242L250,412Q238,424 222,424Q206,424 194,412L108,326Q97,315 97,298Q97,281 108,270Q119,259 136,259Q153,259 164,270L221,327ZM560,680Q543,680 531.5,668.5Q520,657 520,640Q520,623 531.5,611.5Q543,600 560,600L840,600Q857,600 868.5,611.5Q880,623 880,640Q880,657 868.5,668.5Q857,680 840,680L560,680ZM560,360Q543,360 531.5,348.5Q520,337 520,320Q520,303 531.5,291.5Q543,280 560,280L840,280Q857,280 868.5,291.5Q880,303 880,320Q880,337 868.5,348.5Q857,360 840,360L560,360Z",
} as const;

type IconName = keyof typeof paths;
type IconProps = { name: IconName; off?: boolean; label?: string };

export function TaskIcon({ name, off, label }: IconProps) {
  const outlined = name === "rollover";
  return (
    <svg
      className="task-setting-icon"
      viewBox={name === "checklist" ? "0 0 960 960" : "0 0 24 24"}
      aria-hidden={label ? undefined : true}
      role={label ? "img" : undefined}
      aria-label={label}
      focusable="false"
    >
      {label && <title>{label}</title>}
      <path
        d={paths[name]}
        fill={outlined ? "none" : "currentColor"}
        stroke={outlined ? "currentColor" : undefined}
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      {off && (
        <path
          d="M3 3L21 21"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
          strokeLinecap="round"
        />
      )}
    </svg>
  );
}
export function IconLabel({
  name,
  off,
  children,
}: IconProps & { children: ReactNode }) {
  return (
    <span className="task-icon-label">
      <TaskIcon name={name} off={off} />
      <span>{children}</span>
    </span>
  );
}
