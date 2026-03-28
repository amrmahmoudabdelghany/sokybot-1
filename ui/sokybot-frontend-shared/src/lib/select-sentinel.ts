/** Radix Select disallows empty string item values; map "" to this sentinel at the UI boundary. */
export const SELECT_EMPTY_VALUE = "__sokybot_empty__"

export function encodeSelectItemValue(value: string): string {
    return value === "" ? SELECT_EMPTY_VALUE : value
}

export function decodeSelectItemValue(value: string): string {
    return value === SELECT_EMPTY_VALUE ? "" : value
}
