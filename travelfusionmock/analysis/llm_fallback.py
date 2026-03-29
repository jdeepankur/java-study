"""
llm_fallback.py — LLM fallback for constraints the symbolic solver cannot handle.

Called only when solver.py returns used_llm=True.
Sends the method source + the partial constraint tree description
to Claude and asks for valid example strings.
"""

from __future__ import annotations
import json
import re
import textwrap

import anthropic

SYSTEM_PROMPT = textwrap.dedent("""
    You are an expert Java programmer.
    You will be given a Java string validator method (String → boolean).
    Your task is to generate example strings that the method would return true for.

    Respond ONLY with a JSON object — no preamble, no markdown fences:
    {
      "analysis": "one sentence describing what the validator accepts",
      "examples": ["string1", "string2", ...]
    }

    Rules:
    - Produce exactly the requested number of examples
    - Every example MUST return true when passed to the method
    - Make examples diverse in length and structure
    - If the validator is unsatisfiable, set examples to [] and explain in analysis
""").strip()


def generate_with_llm(method_source: str, class_name: str,
                      method_name: str, count: int,
                      partial_reason: str = "") -> dict:
    import os
    api_key = os.environ.get("ANTHROPIC_API_KEY")
    if not api_key:
        raise RuntimeError(
            "No ANTHROPIC_API_KEY environment variable set. "
            "Set it to use the LLM fallback, or simplify the method "
            "so the symbolic solver can handle it."
        )

    client = anthropic.Anthropic(api_key=api_key)

    context = ""
    if partial_reason:
        context = f"\nNote: static analysis was unable to handle: {partial_reason}\n"

    user_message = textwrap.dedent(f"""
        Generate {count} valid strings for this Java validator.
        {context}
        Method: {class_name}.{method_name}

        ```java
        {method_source}
        ```
    """).strip()

    message = client.messages.create(
        model="claude-opus-4-5",
        max_tokens=2048,
        system=SYSTEM_PROMPT,
        messages=[{"role": "user", "content": user_message}]
    )

    raw = message.content[0].text.strip()
    raw = re.sub(r"^```(?:json)?\s*", "", raw)
    raw = re.sub(r"\s*```$", "", raw)

    try:
        return json.loads(raw)
    except json.JSONDecodeError as e:
        raise RuntimeError(f"LLM returned malformed JSON: {e}\nRaw:\n{raw}")
