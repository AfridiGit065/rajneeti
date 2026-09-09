import type { Character } from "@/types/character";

/** Complete game roster. Card artwork at /public/assets/cards/<id>.png */
export const CHARACTERS: readonly Character[] = [
  {
    id: "minister",
    nameBn: "মন্ত্রী",
    nameEn: "THE MINISTER",
    taglineBn: "নীতি নয়, প্রভাবই শক্তি।",
    role: "সংগ্রাহক · Collector",
    description:
      "ক্ষমতার কেন্দ্র থেকে মন্ত্রী রাষ্ট্রের নীতি নির্ধারণ করে সম্পদ সংগ্রহ করতে পারে। রাষ্ট্রযন্ত্র, নীতি এবং সিদ্ধান্ত — সবকিছুই তার হাতের মুঠোয়।",
    imagePath: "/assets/cards/minister.png",
    accent: "gold",
    ability: {
      nameBn: "কর আদায়",
      nameEn: "Tax Collection",
      cost: 0,
      effect: "+ ৩ টাকা",
      effectDetail: "নিজের কোষাগারে ৩ টাকা যোগ করো।",
    },
    block: {
      nameBn: "বিদেশি অনুদান ব্লক",
      nameEn: "Block Foreign Aid",
      detail: "অন্য কোনো খেলোয়াড়ের 'বিদেশি অনুদান' ব্যবহার করতে বাধা দিতে পারে।",
    },
    quote: "Power is a resource.",
  },
  {
    id: "ghatok",
    nameBn: "ঘাতক",
    nameEn: "THE ASSASSIN",
    taglineBn: "নীরবতাই তার শক্তি।",
    role: "আততায়ী · Shadow Striker",
    description:
      "সুযোগ বুঝে সঠিক সময়ে আঘাত হানে ঘাতক। ক্ষমতার খেলায় সে থাকে আড়ালে, কিন্তু তার এক আঘাতেই থেমে যেতে পারে বড় কোনো খেলোয়াড়ের যাত্রা।",
    imagePath: "/assets/cards/ghatok.png",
    accent: "crimson",
    ability: {
      nameBn: "সরিয়ে দেওয়া",
      nameEn: "Assassinate",
      cost: 3,
      effect: "- ৩ টাকা",
      effectDetail: "নিজের ৩ টাকা খরচ করে অন্য কোনো খেলোয়াড়ের একটি প্রভাব কার্ড সরিয়ে দিতে পারে।",
    },
    block: {
      nameBn: "প্রয়োজন নেই",
      nameEn: "No Block",
      detail: "ঘাতকের বিশেষ ক্ষমতা অন্য কোনো চরিত্র দিয়ে ব্লক করা যায় না।",
    },
    quote: "A single move can change everything.",
  },
  {
    id: "dalal",
    nameBn: "দালাল",
    nameEn: "THE BROKER",
    taglineBn: "যোগাযোগই শক্তি।",
    role: "মধ্যস্বত্বভোগী · Middleman",
    description:
      "সরাসরি কিছু করানো না, কিন্তু সঠিক মানুষকে সঠিক সময়ে সঠিক জায়গায় পৌঁছে দিতে পারে। দালালের হাত থাকলে অনেক দরজা খুলে যায়।",
    imagePath: "/assets/cards/dalal.png",
    accent: "forest",
    ability: {
      nameBn: "চুরি",
      nameEn: "Steal",
      cost: 0,
      effect: "+ ২ টাকা",
      effectDetail: "অন্য কোনো খেলোয়াড়ের কাছে থেকে ২ টাকা চুরি করতে পারে।",
    },
    block: {
      nameBn: "রুক করা যায়",
      nameEn: "Can be Blocked",
      detail: "এই চরিত্রের চুরি অ্যাকশনটি আমলা দিয়ে রুক করা যেতে পারে।",
    },
    quote: "Connections move the game.",
  },
  {
    id: "amla",
    nameBn: "আমলা",
    nameEn: "THE BUREAUCRAT",
    taglineBn: "কাগজে আটকে, খেলায় বাঁচে।",
    role: "চক্রপুরুষ · Machine Man",
    description:
      "নিয়ম, প্রক্রিয়া এবং বিলম্ব — এই তিনেই তার আসল শক্তি। সঠিক সময়ে সঠিক ফাইল আটকে দিয়ে সে বড় খেলোয়াড়দের পরিকল্পনা ভেস্তে দিতে পারে।",
    imagePath: "/assets/cards/amla.png",
    accent: "parchment",
    ability: {
      nameBn: "কার্ড বদল",
      nameEn: "Exchange Cards",
      cost: 0,
      effect: "+ ২ কার্ড",
      effectDetail: "ডেক থেকে ২টি কার্ড দেখে নিজের একটি কার্ড বদলাতে পারে।",
    },
    block: {
      nameBn: "অ্যাকশন রুক",
      nameEn: "Block Action",
      detail: "অন্য কোনো খেলোয়াড়ের বিদেশি অনুদান বা ক্ষমতা দখল অ্যাকশন রুক করতে পারে।",
    },
    quote: "Delay is a strategy.",
  },
  {
    id: "goyenda",
    nameBn: "গোয়েন্দা",
    nameEn: "THE DETECTIVE",
    taglineBn: "সত্যের সন্ধানে।",
    role: "তদন্তকারী · Investigator",
    description:
      "তিনি খুঁজে নেন লুকানো সত্য, দেখেন যা অন্যরা দেখে না। ধৈর্য, বিশ্লেষণ আর প্রমাণের সাহায্যে তিনি উন্মোচন করেন ঘটনার আসল রহস্য।",
    imagePath: "/assets/cards/goyenda.png",
    accent: "forest",
    ability: {
      nameBn: "তথ্য উদঘাটন",
      nameEn: "Reveal Info",
      cost: 0,
      effect: "গোপন তথ্য",
      effectDetail: "টার্গেট খেলোয়াড়ের একটি গোপন তথ্য প্রকাশ করা যায়।",
    },
    block: {
      nameBn: "ভুল পথে চালনা",
      nameEn: "Can be Misled",
      detail: "দালাল বা ঠগের প্রভাবে ভুল তথ্য পেতে পারেন।",
    },
    quote: "Evidence never lies.",
  },
];

export const CHARACTER_MAP: Readonly<Record<string, Character>> =
  Object.fromEntries(CHARACTERS.map((c) => [c.id, c]));

export function isCharacterId(value: string): value is Character["id"] {
  return Object.prototype.hasOwnProperty.call(CHARACTER_MAP, value);
}
