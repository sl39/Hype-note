type Props = {
  text: string;
  onClick: () => void;
};
export default function Button({ text, onClick }: Props) {
  return (
    <button
      type="submit"
      className="flex w-full justify-center rounded-md bg-[#2946A2] px-3 py-1.5 text-sm font-semibold leading-6 text-white shadow-sm hover:bg-[#375dd4] focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-indigo-600"
    >
      {text}
    </button>
  );
}
