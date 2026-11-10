import en from "./en.json";
import fr from "./fr.json";
import { iconLabels } from "./shared-icons";
export function locale(language: string) {
  return language === "system"
    ? navigator.language.startsWith("fr")
      ? "fr"
      : "en"
    : language === "fr"
      ? "fr"
      : "en";
}
export function translator(language: string) {
  const lang = locale(language);
  const dict: Record<string, string> = lang === "fr" ? fr : en;
  return (key: string) =>
    key.startsWith("categoryIcon.")
      ? (iconLabels[lang][key.slice(13)] ?? iconLabels.en[key.slice(13)] ?? key)
      : (dict[key] ?? (en as Record<string, string>)[key] ?? key);
}
export type Text = (key: string) => string;
