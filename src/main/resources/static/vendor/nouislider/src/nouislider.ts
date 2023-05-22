"use strict";

interface CssClasses {
    target: string;
    base: string;
    origin: string;
    handle: string;
    handleLower: string;
    handleUpper: string;
    touchArea: string;
    horizontal: string;
    vertical: string;
    background: string;
    connect: string;
    connects: string;
    ltr: string;
    rtl: string;
    textDirectionLtr: string;
    textDirectionRtl: string;
    draggable: string;
    drag: string;
    tap: string;
    active: string;
    tooltip: string;
    pips: string;
    pipsHorizontal: string;
    pipsVertical: string;
    marker: string;
    markerHorizontal: string;
    markerVertical: string;
    markerNormal: string;
    markerLarge: string;
    markerSub: string;
    value: string;
    valueHorizontal: string;
    valueVertical: string;
    valueNormal: string;
    valueLarge: string;
    valueSub: string;
}

export interface PartialFormatter {
    to: (value: number) => string | number;
    from?: (value: string) => number | false;
}

export interface Formatter extends PartialFormatter {
    from: (value: string) => number | false;
}

export enum PipsMode {
    Range = "range",
    Steps = "steps",
    Positions = "positions",
    Count = "count",
    Values = "values",
}

export enum PipsType {
    None = -1,
    NoValue = 0,
    LargeValue = 1,
    SmallValue = 2,
}

type WrappedSubRange = [number] | [number, number];

type SubRange = number | WrappedSubRange;

interface Range {
    min: SubRange;
    max: SubRange;
    [key: string]: SubRange;
}

//region Pips

interface BasePips {
    mode: PipsMode;
    density?: number;
    filter?: PipsFilter;
    format?: PartialFormatter;
}

interface PositionsPips extends BasePips {
    mode: PipsMode.Positions;
    values: number[];
    stepped?: boolean;
}

interface ValuesPips extends BasePips {
    mode: PipsMode.Values;
    values: number[];
    stepped?: boolean;
}

interface CountPips extends BasePips {
    mode: PipsMode.Count;
    values: number;
    stepped?: boolean;
}

interface StepsPips extends BasePips {
    mode: PipsMode.Steps;
}

interface RangePips extends BasePips {
    mode: PipsMode.Range;
}

type Pips = PositionsPips | ValuesPips | CountPips | StepsPips | RangePips;

//endregion

type StartValues = string | number | (string | number)[];

type HandleAttributes = { [key: string]: string };

interface UpdatableOptions {
    range?: Range;
    start?: StartValues;
    margin?: number;
    limit?: number;
    padding?: number | number[];
    snap?: boolean;
    step?: number;
    pips?: Pips;
    format?: Formatter;
    tooltips?: boolean | PartialFormatter | (boolean | PartialFormatter)[];
    animate?: boolean;
}

export interface Options extends UpdatableOptions {
    range: Range;
    connect?: "lower" | "upper" | boolean | boolean[];
    orientation?: "vertical" | "horizontal";
    direction?: "ltr" | "rtl";
    behaviour?: string;
    keyboardSupport?: boolean;
    keyboardPageMultiplier?: number;
    keyboardMultiplier?: number;
    keyboardDefaultStep?: number;
    documentElement?: HTMLElement;
    cssPrefix?: string;
    cssClasses?: CssClasses;
    ariaFormat?: PartialFormatter;
    animationDuration?: number;
    handleAttributes?: HandleAttributes[];
}

interface Behaviour {
    tap: boolean;
    drag: boolean;
    dragAll: boolean;
    smoothSteps: boolean;
    fixed: boolean;
    snap: boolean;
    hover: boolean;
    unconstrained: boolean;
}

interface ParsedOptions {
    animate: boolean;
    connect: boolean[];
    start: number[];
    margin: number[] | null;
    limit: number[] | null;
    padding: number[][] | null;
    step?: number;
    orientation?: "vertical" | "horizontal";
    direction?: "ltr" | "rtl";
    tooltips?: (boolean | PartialFormatter)[];
    keyboardSupport: boolean;
    keyboardPageMultiplier: number;
    keyboardMultiplier: number;
    keyboardDefaultStep: number;
    documentElement?: HTMLElement;
    cssPrefix?: string | false;
    cssClasses: CssClasses;
    ariaFormat: PartialFormatter;
    pips?: Pips;
    animationDuration: number;
    snap?: boolean;
    format: Formatter;
    handleAttributes?: HandleAttributes[];

    range: Range;
    singleStep: number;
    transformRule: "transform" | "msTransform" | "webkitTransform";
    style: "left" | "top" | "right" | "bottom";
    ort: 0 | 1;
    handles: number;
    events: Behaviour;
    dir: 0 | 1;
    spectrum: Spectrum;
}

export interface API {
    destroy: () => void;
    steps: () => NextStepsForHandle[];
    on: (eventName: string, callback: EventCallback) => void;
    off: (eventName: string) => void;
    get: (unencoded?: boolean) => GetResult;
    set: (input: number | string | (number | string)[], fireSetEvent?: boolean, exactInput?: boolean) => void;
    setHandle: (handleNumber: number, value: number | string, fireSetEvent?: boolean, exactInput?: boolean) => void;
    reset: (fireSetEvent?: boolean) => void;
    options: Options;
    updateOptions: (optionsToUpdate: UpdatableOptions, fireSetEvent: boolean) => void;
    target: HTMLElement;
    removePips: () => void;
    removeTooltips: () => void;
    getTooltips: () => { [handleNumber: number]: HTMLElement | false };
    getOrigins: () => { [handleNumber: number]: HTMLElement };
    pips: (grid: Pips) => HTMLElement;
}

interface TargetElement extends HTMLElement {
    noUiSlider?: API;
}

interface CSSStyleDeclarationIE10 extends CSSStyleDeclaration {
    msTransform?: string;
}

interface EventData {
    target?: HTMLElement;
    handles?: HTMLElement[];
    handle?: HTMLElement;
    connect?: HTMLElement;
    listeners?: [string, EventHandler][];
    startCalcPoint?: number;
    baseSize?: number;
    pageOffset?: PageOffset;
    handleNumbers: number[];
    buttonsProperty?: number;
    locations?: number[];
    doNotReject?: boolean;
    hover?: boolean;
}

interface MoveEventData extends EventData {
    listeners: [string, EventHandler][];
    startCalcPoint: number;
    baseSize: number;
    locations: number[];
}

interface EndEventData extends EventData {
    listeners: [string, EventHandler][];
}

interface NearByStep {
    startValue: number;
    step: number | false;
    highestStep: number;
}

interface NearBySteps {
    stepBefore: NearByStep;
    thisStep: NearByStep;
    stepAfter: NearByStep;
}

type EventHandler = (event: BrowserEvent) => false | undefined;

type GetResult = number | string | (string | number)[];

type NextStepsForHandle = [number | false | null, number | false | null];

type OptionKey = keyof Options & keyof ParsedOptions & keyof UpdatableOptions;

type PipsFilter = (value: number, type: PipsType) => PipsType;

type PageOffset = { x: number; y: number };

type BrowserEvent = MouseEvent &
    TouchEvent & { pageOffset: PageOffset; points: [number, number]; cursor: boolean; calcPoint: number };

type EventCallback = (
    this: API,
    values: (number | string)[],
    handleNumber: number,
    unencoded: number[],
    tap: boolean,
    locations: number[],
    slider: API
) => void;

//region Helper Methods

function isValidFormatter(entry: unknown): entry is Formatter {
    return isValidPartialFormatter(entry) && typeof (<Formatter>entry).from === "function";
}

function isValidPartialFormatter(entry: unknown): entry is PartialFormatter {
    // partial formatters only need a to function and not a from function
    return typeof entry === "object" && typeof (<Formatter>entry).to === "function";
}

function removeElement(el: HTMLElement): void {
    (el.parentElement as HTMLElement).removeChild(el);
}

function isSet<T>(value: T): value is Exclude<T, null | undefined> {
    return value !== null && value !== undefined;
}

// Bindable version
function preventDefault(e: Event): void {
    e.preventDefault();
}

// Removes duplicates from an array.
function unique<Type>(array: Type[]): Type[] {
    return array.filter(function (a) {
        return !this[a] ? (this[a] = true) : false;
    }, {});
}

// Round a value to the closest 'to'.
function closest(value: number, to: number): number {
    return Math.round(value / to) * to;
}

// Current position of an element relative to the document.
function offset(elem: HTMLElement, orientation: 0 | 1): number {
    const rect = elem.getBoundingClientRect();
    const doc = elem.ownerDocument;
    const docElem = doc.documentElement;
    const pageOffset = getPageOffset(doc);

    // getBoundingClientRect contains left scroll in Chrome on Android.
    // I haven't found a feature detection that proves this. Worst case
    // scenario on mis-match: the 'tap' feature on horizontal sliders breaks.
    if (/webkit.*Chrome.*Mobile/i.test(navigator.userAgent)) {
        pageOffset.x = 0;
    }

    return orientation ? rect.top + pageOffset.y - docElem.clientTop : rect.left + pageOffset.x - docElem.clientLeft;
}

// Checks whether a value is numerical.
function isNumeric(a: unknown): a is number {
    return typeof a === "number" && !isNaN(a) && isFinite(a);
}

// Sets a class and removes it after [duration] ms.
function addClassFor(element: HTMLElement, className: string, duration: number): void {
    if (duration > 0) {
        addClass(element, className);
        setTimeout(function () {
            removeClass(element, className);
        }, duration);
    }
}

// Limits a value to 0 - 100
function limit(a: number): number {
    return Math.max(Math.min(a, 100), 0);
}

// Wraps a variable as an array, if it isn't one yet.
// Note that an input array is returned by reference!
function asArray<Type>(a: Type | Type[]): Type[] {
    return Array.isArray(a) ? a : [a];
}

// Counts decimals
function countDecimals(numStr: string | number | false): number {
    numStr = String(numStr);
    const pieces = numStr.split(".");
    return pieces.length > 1 ? pieces[1].length : 0;
}

// http://youmightnotneedjquery.com/#add_class
function addClass(el: HTMLElement, className: string): void {
    if (el.classList && !/\s/.test(className)) {
        el.classList.add(className);
    } else {
        el.className += " " + className;
    }
}

// http://youmightnotneedjquery.com/#remove_class
function removeClass(el: HTMLElement, className: string): void {
    if (el.classList && !/\s/.test(className)) {
        el.classList.remove(className);
    } else {
        el.className = el.className.replace(
            new RegExp("(^|\\b)" + className.split(" ").join("|") + "(\\b|$)", "gi"),
            " "
        );
    }
}

// https://plainjs.com/javascript/attributes/adding-removing-and-testing-for-classes-9/
function hasClass(el: HTMLElement, className: string): boolean {
    return el.classList ? el.classList.contains(className) : new RegExp("\\b" + className + "\\b").test(el.className);
}

// https://developer.mozilla.org/en-US/docs/Web/API/Window/scrollY#Notes
function getPageOffset(doc: Document): PageOffset {
    const supportPageOffset = window.pageXOffset !== undefined;
    const isCSS1Compat = (doc.compatMode || "") === "CSS1Compat";
    const x = supportPageOffset
        ? window.pageXOffset
        : isCSS1Compat
        ? doc.documentElement.scrollLeft
        : doc.body.scrollLeft;
    const y = supportPageOffset
        ? window.pageYOffset
        : isCSS1Compat
        ? doc.documentElement.scrollTop
        : doc.body.scrollTop;

    return {
        x: x,
        y: y,
    };
}

// we provide a function to compute constants instead
// of accessing window.* as soon as the module needs it
// so that we do not compute anything if not needed
function getActions(): { start: string; move: string; end: string } {
    // Determine the events to bind. IE11 implements pointerEvents without
    // a prefix, which breaks compatibility with the IE10 implementation.
    return (window.navigator as any).pointerEnabled
        ? {
              start: "pointerdown",
              move: "pointermove",
              end: "pointerup",
          }
        : (window.navigator as any).msPointerEnabled
        ? {
              start: "MSPointerDown",
              move: "MSPointerMove",
              end: "MSPointerUp",
          }
        : {
              start: "mousedown touchstart",
              move: "mousemove touchmove",
              end: "mouseup touchend",
          };
}

// https://github.com/WICG/EventListenerOptions/blob/gh-pages/explainer.md
// Issue #785
function getSupportsPassive(): boolean {
    let supportsPassive = false;

    /* eslint-disable */
    try {
        const opts = Object.defineProperty({}, "passive", {
            get: function () {
                supportsPassive = true;
            },
        });

        // @ts-ignore
        window.addEventListener("test", null, opts);
    } catch (e) {}
    /* eslint-enable */

    return supportsPassive;
}

function getSupportsTouchActionNone(): boolean {
    return window.CSS && CSS.supports && CSS.supports("touch-action", "none");
}

//endregion

//region Range Calculation

// Determine the size of a sub-range in relation to a full range.
function subRangeRatio(pa: number, pb: number): number {
    return 100 / (pb - pa);
}

// (percentage) How many percent is this value of this range?
function fromPercentage(range: number[], value: number, startRange: number): number {
    return (value * 100) / (range[startRange + 1] - range[startRange]);
}

// (percentage) Where is this value on this range?
function toPercentage(range: [number, number], value: number): number {
    return fromPercentage(range, range[0] < 0 ? value + Math.abs(range[0]) : value - range[0], 0);
}

// (value) How much is this percentage on this range?
function isPercentage(range: [number, number], value: number): number {
    return (value * (range[1] - range[0])) / 100 + range[0];
}

function getJ(value: number, arr: number[]): number {
    let j = 1;

    while (value >= arr[j]) {
        j += 1;
    }

    return j;
}

// (percentage) Input a value, find where, on a scale of 0-100, it applies.
function toStepping(xVal: number[], xPct: number[], value: number): number {
    if (value >= xVal.slice(-1)[0]) {
        return 100;
    }

    const j = getJ(value, xVal);
    const va = xVal[j - 1];
    const vb = xVal[j];
    const pa = xPct[j - 1];
    const pb = xPct[j];

    return pa + toPercentage([va, vb], value) / subRangeRatio(pa, pb);
}

// (value) Input a percentage, find where it is on the specified range.
function fromStepping(xVal: number[], xPct: number[], value: number): number {
    // There is no range group that fits 100
    if (value >= 100) {
        return xVal.slice(-1)[0];
    }

    const j = getJ(value, xPct);
    const va = xVal[j - 1];
    const vb = xVal[j];
    const pa = xPct[j - 1];
    const pb = xPct[j];

    return isPercentage([va, vb], (value - pa) * subRangeRatio(pa, pb));
}

// (percentage) Get the step that applies at a certain value.
function getStep(xPct: number[], xSteps: (number | false)[], snap: boolean, value: number): number {
    if (value === 100) {
        return value;
    }

    const j = getJ(value, xPct);
    const a = xPct[j - 1];
    const b = xPct[j];

    // If 'snap' is set, steps are used as fixed points on the slider.
    if (snap) {
        // Find the closest position, a or b.
        if (value - a > (b - a) / 2) {
            return b;
        }

        return a;
    }

    if (!xSteps[j - 1]) {
        return value;
    }

    return xPct[j - 1] + closest(value - xPct[j - 1], xSteps[j - 1] as number);
}

//endregion

//region Spectrum

class Spectrum {
    public xPct: number[] = [];
    public xVal: number[] = [];
    public xSteps: (number | false)[] = [];
    public xNumSteps: (number | false)[] = [];
    protected xHighestCompleteStep: number[] = [];
    protected snap: boolean;

    constructor(entry: Range, snap: boolean, singleStep: number) {
        this.xSteps = [singleStep || false];
        this.xNumSteps = [false];

        this.snap = snap;

        let index;
        const ordered: [WrappedSubRange, string][] = [];

        // Map the object keys to an array.
        Object.keys(entry).forEach((index) => {
            ordered.push([asArray(entry[index]) as WrappedSubRange, index]);
        });

        // Sort all entries by value (numeric sort).
        ordered.sort(function (a, b) {
            return a[0][0] - b[0][0];
        });

        // Convert all entries to subranges.
        for (index = 0; index < ordered.length; index++) {
            this.handleEntryPoint(ordered[index][1], ordered[index][0]);
        }

        // Store the actual step values.
        // xSteps is sorted in the same order as xPct and xVal.
        this.xNumSteps = this.xSteps.slice(0);

        // Convert all numeric steps to the percentage of the subrange they represent.
        for (index = 0; index < this.xNumSteps.length; index++) {
            this.handleStepPoint(index, this.xNumSteps[index]);
        }
    }

    public getDistance(value: number): number[] {
        const distances = [];

        for (let index = 0; index < this.xNumSteps.length - 1; index++) {
            distances[index] = fromPercentage(this.xVal, value, index);
        }

        return distances;
    }

    // Calculate the percentual distance over the whole scale of ranges.
    // direction: 0 = backwards / 1 = forwards
    public getAbsoluteDistance(value: number, distances: number[] | null, direction: boolean): number {
        let xPct_index = 0;

        // Calculate range where to start calculation
        if (value < this.xPct[this.xPct.length - 1]) {
            while (value > this.xPct[xPct_index + 1]) {
                xPct_index++;
            }
        } else if (value === this.xPct[this.xPct.length - 1]) {
            xPct_index = this.xPct.length - 2;
        }

        // If looking backwards and the value is exactly at a range separator then look one range further
        if (!direction && value === this.xPct[xPct_index + 1]) {
            xPct_index++;
        }

        if (distances === null) {
            distances = [];
        }

        let start_factor;
        let rest_factor = 1;

        let rest_rel_distance = distances[xPct_index];

        let range_pct = 0;

        let rel_range_distance = 0;
        let abs_distance_counter = 0;
        let range_counter = 0;

        // Calculate what part of the start range the value is
        if (direction) {
            start_factor = (value - this.xPct[xPct_index]) / (this.xPct[xPct_index + 1] - this.xPct[xPct_index]);
        } else {
            start_factor = (this.xPct[xPct_index + 1] - value) / (this.xPct[xPct_index + 1] - this.xPct[xPct_index]);
        }

        // Do until the complete distance across ranges is calculated
        while (rest_rel_distance > 0) {
            // Calculate the percentage of total range
            range_pct = this.xPct[xPct_index + 1 + range_counter] - this.xPct[xPct_index + range_counter];

            // Detect if the margin, padding or limit is larger then the current range and calculate
            if (distances[xPct_index + range_counter] * rest_factor + 100 - start_factor * 100 > 100) {
                // If larger then take the percentual distance of the whole range
                rel_range_distance = range_pct * start_factor;
                // Rest factor of relative percentual distance still to be calculated
                rest_factor = (rest_rel_distance - 100 * start_factor) / distances[xPct_index + range_counter];
                // Set start factor to 1 as for next range it does not apply.
                start_factor = 1;
            } else {
                // If smaller or equal then take the percentual distance of the calculate percentual part of that range
                rel_range_distance = ((distances[xPct_index + range_counter] * range_pct) / 100) * rest_factor;
                // No rest left as the rest fits in current range
                rest_factor = 0;
            }

            if (direction) {
                abs_distance_counter = abs_distance_counter - rel_range_distance;
                // Limit range to first range when distance becomes outside of minimum range
                if (this.xPct.length + range_counter >= 1) {
                    range_counter--;
                }
            } else {
                abs_distance_counter = abs_distance_counter + rel_range_distance;
                // Limit range to last range when distance becomes outside of maximum range
                if (this.xPct.length - range_counter >= 1) {
                    range_counter++;
                }
            }

            // Rest of relative percentual distance still to be calculated
            rest_rel_distance = distances[xPct_index + range_counter] * rest_factor;
        }

        return value + abs_distance_counter;
    }

    public toStepping(value: number): number {
        value = toStepping(this.xVal, this.xPct, value);

        return value;
    }

    public fromStepping(value: number): number {
        return fromStepping(this.xVal, this.xPct, value);
    }

    public getStep(value: number): number {
        value = getStep(this.xPct, this.xSteps, this.snap, value);

        return value;
    }

    public getDefaultStep(value: number, isDown: boolean, size: number): number {
        let j = getJ(value, this.xPct);

        // When at the top or stepping down, look at the previous sub-range
        if (value === 100 || (isDown && value === this.xPct[j - 1])) {
            j = Math.max(j - 1, 1);
        }

        return (this.xVal[j] - this.xVal[j - 1]) / size;
    }

    public getNearbySteps(value: number): NearBySteps {
        const j = getJ(value, this.xPct);

        return {
            stepBefore: {
                startValue: this.xVal[j - 2],
                step: this.xNumSteps[j - 2],
                highestStep: this.xHighestCompleteStep[j - 2],
            },
            thisStep: {
                startValue: this.xVal[j - 1],
                step: this.xNumSteps[j - 1],
                highestStep: this.xHighestCompleteStep[j - 1],
            },
            stepAfter: {
                startValue: this.xVal[j],
                step: this.xNumSteps[j],
                highestStep: this.xHighestCompleteStep[j],
            },
        };
    }

    public countStepDecimals(): number {
        const stepDecimals = this.xNumSteps.map(countDecimals);
        return Math.max.apply(null, stepDecimals);
    }

    public hasNoSize(): boolean {
        return this.xVal[0] === this.xVal[this.xVal.length - 1];
    }

    // Outside testing
    public convert(value: number): number {
        return this.getStep(this.toStepping(value));
    }

    private handleEntryPoint(index: string, value: WrappedSubRange): void {
        let percentage;

        // Covert min/max syntax to 0 and 100.
        if (index === "min") {
            percentage = 0;
        } else if (index === "max") {
            percentage = 100;
        } else {
            percentage = parseFloat(index);
        }

        // Check for correct input.
        if (!isNumeric(percentage) || !isNumeric(value[0])) {
            throw new Error("noUiSlider: 'range' value isn't numeric.");
        }

        // Store values.
        this.xPct.push(percentage);
        this.xVal.push(value[0]);

        const value1 = Number(value[1]);

        // NaN will evaluate to false too, but to keep
        // logging clear, set step explicitly. Make sure
        // not to override the 'step' setting with false.
        if (!percentage) {
            if (!isNaN(value1)) {
                this.xSteps[0] = value1;
            }
        } else {
            this.xSteps.push(isNaN(value1) ? false : value1);
        }

        this.xHighestCompleteStep.push(0);
    }

    private handleStepPoint(i: number, n: number | false): void {
        // Ignore 'false' stepping.
        if (!n) {
            return;
        }

        // Step over zero-length ranges (#948);
        if (this.xVal[i] === this.xVal[i + 1]) {
            this.xSteps[i] = this.xHighestCompleteStep[i] = this.xVal[i];

            return;
        }

        // Factor to range ratio
        this.xSteps[i] =
            fromPercentage([this.xVal[i], this.xVal[i + 1]], n, 0) / subRangeRatio(this.xPct[i], this.xPct[i + 1]);

        const totalSteps = (this.xVal[i + 1] - this.xVal[i]) / (this.xNumSteps[i] as number);
        const highestStep = Math.ceil(Number(totalSteps.toFixed(3)) - 1);
        const step = this.xVal[i] + (this.xNumSteps[i] as number) * highestStep;

        this.xHighestCompleteStep[i] = step;
    }
}

//endregion

//region Options

/*	Every input option is tested and parsed. This will prevent
    endless validation in internal methods. These tests are
    structured with an item for every option available. An
    option can be marked as required by setting the 'r' flag.
    The testing function is provided with three arguments:
        - The provided value for the option;
        - A reference to the options object;
        - The name for the option;

    The testing function returns false when an error is detected,
    or true when everything is OK. It can also modify the option
    object, to make sure all values can be correctly looped elsewhere. */

//region Defaults

const defaultFormatter: Formatter = {
    to: function (value) {
        return value === undefined ? "" : value.toFixed(2);
    },
    from: Number,
};

const cssClasses: CssClasses = {
    target: "target",
    base: "base",
    origin: "origin",
    handle: "handle",
    handleLower: "handle-lower",
    handleUpper: "handle-upper",
    touchArea: "touch-area",
    horizontal: "horizontal",
    vertical: "vertical",
    background: "background",
    connect: "connect",
    connects: "connects",
    ltr: "ltr",
    rtl: "rtl",
    textDirectionLtr: "txt-dir-ltr",
    textDirectionRtl: "txt-dir-rtl",
    draggable: "draggable",
    drag: "state-drag",
    tap: "state-tap",
    active: "active",
    tooltip: "tooltip",
    pips: "pips",
    pipsHorizontal: "pips-horizontal",
    pipsVertical: "pips-vertical",
    marker: "marker",
    markerHorizontal: "marker-horizontal",
    markerVertical: "marker-vertical",
    markerNormal: "marker-normal",
    markerLarge: "marker-large",
    markerSub: "marker-sub",
    value: "value",
    valueHorizontal: "value-horizontal",
    valueVertical: "value-vertical",
    valueNormal: "value-normal",
    valueLarge: "value-large",
    valueSub: "value-sub",
};

// Namespaces of internal event listeners
const INTERNAL_EVENT_NS = {
    tooltips: ".__tooltips",
    aria: ".__aria",
};

//endregion

function testStep(parsed: ParsedOptions, entry: unknown): void {
    if (!isNumeric(entry)) {
        throw new Error("noUiSlider: 'step' is not numeric.");
    }

    // The step option can still be used to set stepping
    // for linear sliders. Overwritten if set in 'range'.
    parsed.singleStep = entry;
}

function testKeyboardPageMultiplier(parsed: ParsedOptions, entry: unknown): void {
    if (!isNumeric(entry)) {
        throw new Error("noUiSlider: 'keyboardPageMultiplier' is not numeric.");
    }

    parsed.keyboardPageMultiplier = entry;
}

function testKeyboardMultiplier(parsed: ParsedOptions, entry: unknown): void {
    if (!isNumeric(entry)) {
        throw new Error("noUiSlider: 'keyboardMultiplier' is not numeric.");
    }

    parsed.keyboardMultiplier = entry;
}

function testKeyboardDefaultStep(parsed: ParsedOptions, entry: unknown): void {
    if (!isNumeric(entry)) {
        throw new Error("noUiSlider: 'keyboardDefaultStep' is not numeric.");
    }

    parsed.keyboardDefaultStep = entry;
}

function testRange(parsed: ParsedOptions, entry: Range): void {
    // Filter incorrect input.
    if (typeof entry !== "object" || Array.isArray(entry)) {
        throw new Error("noUiSlider: 'range' is not an object.");
    }

    // Catch missing start or end.
    if (entry.min === undefined || entry.max === undefined) {
        throw new Error("noUiSlider: Missing 'min' or 'max' in 'range'.");
    }

    parsed.spectrum = new Spectrum(entry, parsed.snap || false, parsed.singleStep);
}

function testStart(parsed: ParsedOptions, entry: unknown): void {
    entry = asArray(entry);

    // Validate input. Values aren't tested, as the public .val method
    // will always provide a valid location.
    if (!Array.isArray(entry) || !entry.length) {
        throw new Error("noUiSlider: 'start' option is incorrect.");
    }

    // Store the number of handles.
    parsed.handles = entry.length;

    // When the slider is initialized, the .val method will
    // be called with the start options.
    parsed.start = entry;
}

function testSnap(parsed: ParsedOptions, entry: unknown): void {
    if (typeof entry !== "boolean") {
        throw new Error("noUiSlider: 'snap' option must be a boolean.");
    }

    // Enforce 100% stepping within subranges.
    parsed.snap = entry;
}

function testAnimate(parsed: ParsedOptions, entry: unknown): void {
    if (typeof entry !== "boolean") {
        throw new Error("noUiSlider: 'animate' option must be a boolean.");
    }

    // Enforce 100% stepping within subranges.
    parsed.animate = entry;
}

function testAnimationDuration(parsed: ParsedOptions, entry: unknown): void {
    if (typeof entry !== "number") {
        throw new Error("noUiSlider: 'animationDuration' option must be a number.");
    }

    parsed.animationDuration = entry;
}

function testConnect(parsed: ParsedOptions, entry: unknown): void {
    let connect = [false];
    let i;

    // Map legacy options
    if (entry === "lower") {
        entry = [true, false];
    } else if (entry === "upper") {
        entry = [false, true];
    }

    // Handle boolean options
    if (entry === true || entry === false) {
        for (i = 1; i < parsed.handles; i++) {
            connect.push(entry);
        }

        connect.push(false);
    }

    // Reject invalid input
    else if (!Array.isArray(entry) || !entry.length || entry.length !== parsed.handles + 1) {
        throw new Error("noUiSlider: 'connect' option doesn't match handle count.");
    } else {
        connect = entry;
    }

    parsed.connect = connect;
}

function testOrientation(parsed: ParsedOptions, entry: unknown): void {
    // Set orientation to an a numerical value for easy
    // array selection.
    switch (entry) {
        case "horizontal":
            parsed.ort = 0;
            break;
        case "vertical":
            parsed.ort = 1;
            break;
        default:
            throw new Error("noUiSlider: 'orientation' option is invalid.");
    }
}

function testMargin(parsed: ParsedOptions, entry: unknown): void {
    if (!isNumeric(entry)) {
        throw new Error("noUiSlider: 'margin' option must be numeric.");
    }

    // Issue #582
    if (entry === 0) {
        return;
    }

    parsed.margin = parsed.spectrum.getDistance(entry);
}

function testLimit(parsed: ParsedOptions, entry: unknown): void {
    if (!isNumeric(entry)) {
        throw new Error("noUiSlider: 'limit' option must be numeric.");
    }

    parsed.limit = parsed.spectrum.getDistance(entry);

    if (!parsed.limit || parsed.handles < 2) {
        throw new Error("noUiSlider: 'limit' option is only supported on linear sliders with 2 or more handles.");
    }
}

function testPadding(parsed: ParsedOptions, entry: number | [number, number]): void {
    let index;

    if (!isNumeric(entry) && !Array.isArray(entry)) {
        throw new Error("noUiSlider: 'padding' option must be numeric or array of exactly 2 numbers.");
    }

    if (Array.isArray(entry) && !(entry.length === 2 || isNumeric(entry[0]) || isNumeric(entry[1]))) {
        throw new Error("noUiSlider: 'padding' option must be numeric or array of exactly 2 numbers.");
    }

    if (entry === 0) {
        return;
    }

    if (!Array.isArray(entry)) {
        entry = [entry, entry];
    }

    // 'getDistance' returns false for invalid values.
    parsed.padding = [parsed.spectrum.getDistance(entry[0]), parsed.spectrum.getDistance(entry[1])];

    for (index = 0; index < parsed.spectrum.xNumSteps.length - 1; index++) {
        // last "range" can't contain step size as it is purely an endpoint.
        if (parsed.padding[0][index] < 0 || parsed.padding[1][index] < 0) {
            throw new Error("noUiSlider: 'padding' option must be a positive number(s).");
        }
    }

    const totalPadding = entry[0] + entry[1];
    const firstValue = parsed.spectrum.xVal[0];
    const lastValue = parsed.spectrum.xVal[parsed.spectrum.xVal.length - 1];

    if (totalPadding / (lastValue - firstValue) > 1) {
        throw new Error("noUiSlider: 'padding' option must not exceed 100% of the range.");
    }
}

function testDirection(parsed: ParsedOptions, entry: unknown): void {
    // Set direction as a numerical value for easy parsing.
    // Invert connection for RTL sliders, so that the proper
    // handles get the connect/background classes.
    switch (entry) {
        case "ltr":
            parsed.dir = 0;
            break;
        case "rtl":
            parsed.dir = 1;
            break;
        default:
            throw new Error("noUiSlider: 'direction' option was not recognized.");
    }
}

function testBehaviour(parsed: ParsedOptions, entry: unknown): void {
    // Make sure the input is a string.
    if (typeof entry !== "string") {
        throw new Error("noUiSlider: 'behaviour' must be a string containing options.");
    }

    // Check if the string contains any keywords.
    // None are required.
    const tap = entry.indexOf("tap") >= 0;
    const drag = entry.indexOf("drag") >= 0;
    const fixed = entry.indexOf("fixed") >= 0;
    const snap = entry.indexOf("snap") >= 0;
    const hover = entry.indexOf("hover") >= 0;
    const unconstrained = entry.indexOf("unconstrained") >= 0;
    const dragAll = entry.indexOf("drag-all") >= 0;
    const smoothSteps = entry.indexOf("smooth-steps") >= 0;

    if (fixed) {
        if (parsed.handles !== 2) {
            throw new Error("noUiSlider: 'fixed' behaviour must be used with 2 handles");
        }

        // Use margin to enforce fixed state
        testMargin(parsed, parsed.start[1] - parsed.start[0]);
    }

    if (unconstrained && (parsed.margin || parsed.limit)) {
        throw new Error("noUiSlider: 'unconstrained' behaviour cannot be used with margin or limit");
    }

    parsed.events = {
        tap: tap || snap,
        drag: drag,
        dragAll: dragAll,
        smoothSteps: smoothSteps,
        fixed: fixed,
        snap: snap,
        hover: hover,
        unconstrained: unconstrained,
    };
}

function testTooltips(parsed: ParsedOptions, entry: boolean | Formatter | (boolean | Formatter)[]): void {
    if (entry === false) {
        return;
    }

    if (entry === true || isValidPartialFormatter(entry)) {
        parsed.tooltips = [];

        for (let i = 0; i < parsed.handles; i++) {
            parsed.tooltips.push(entry);
        }
    } else {
        entry = asArray(entry);

        if (entry.length !== parsed.handles) {
            throw new Error("noUiSlider: must pass a formatter for all handles.");
        }

        entry.forEach(function (formatter) {
            if (typeof formatter !== "boolean" && !isValidPartialFormatter(formatter)) {
                throw new Error("noUiSlider: 'tooltips' must be passed a formatter or 'false'.");
            }
        });

        parsed.tooltips = entry;
    }
}

function testHandleAttributes(parsed: ParsedOptions, entry: HandleAttributes[]): void {
    if (entry.length !== parsed.handles) {
        throw new Error("noUiSlider: must pass a attributes for all handles.");
    }

    parsed.handleAttributes = entry;
}

function testAriaFormat(parsed: ParsedOptions, entry: PartialFormatter): void {
    if (!isValidPartialFormatter(entry)) {
        throw new Error("noUiSlider: 'ariaFormat' requires 'to' method.");
    }

    parsed.ariaFormat = entry;
}

function testFormat(parsed: ParsedOptions, entry: Formatter): void {
    if (!isValidFormatter(entry)) {
        throw new Error("noUiSlider: 'format' requires 'to' and 'from' methods.");
    }

    parsed.format = entry;
}

function testKeyboardSupport(parsed: ParsedOptions, entry: unknown): void {
    if (typeof entry !== "boolean") {
        throw new Error("noUiSlider: 'keyboardSupport' option must be a boolean.");
    }

    parsed.keyboardSupport = entry;
}

function testDocumentElement(parsed: ParsedOptions, entry: HTMLElement): void {
    // This is an advanced option. Passed values are used without validation.
    parsed.documentElement = entry;
}

function testCssPrefix(parsed: ParsedOptions, entry: unknown): void {
    if (typeof entry !== "string" && entry !== false) {
        throw new Error("noUiSlider: 'cssPrefix' must be a string or `false`.");
    }

    parsed.cssPrefix = entry;
}

function testCssClasses(parsed: ParsedOptions, entry: CssClasses): void {
    if (typeof entry !== "object") {
        throw new Error("noUiSlider: 'cssClasses' must be an object.");
    }

    if (typeof parsed.cssPrefix === "string") {
        parsed.cssClasses = {} as CssClasses;

        Object.keys(entry).forEach((key: keyof CssClasses) => {
            parsed.cssClasses[key] = parsed.cssPrefix + entry[key];
        });
    } else {
        parsed.cssClasses = entry;
    }
}

// Test all developer settings and parse to assumption-safe values.
function testOptions(options: Options): ParsedOptions {
    // To prove a fix for #537, freeze options here.
    // If the object is modified, an error will be thrown.
    // Object.freeze(options);

    const parsed = {
        margin: null,
        limit: null,
        padding: null,
        animate: true,
        animationDuration: 300,
        ariaFormat: defaultFormatter,
        format: defaultFormatter,
    } as ParsedOptions;

    // Tests are executed in the order they are presented here.
    const tests: { [key in keyof Options]: { r: boolean; t: (parsed: ParsedOptions, entry: unknown) => void } } = {
        step: { r: false, t: testStep },
        keyboardPageMultiplier: { r: false, t: testKeyboardPageMultiplier },
        keyboardMultiplier: { r: false, t: testKeyboardMultiplier },
        keyboardDefaultStep: { r: false, t: testKeyboardDefaultStep },
        start: { r: true, t: testStart },
        connect: { r: true, t: testConnect },
        direction: { r: true, t: testDirection },
        snap: { r: false, t: testSnap },
        animate: { r: false, t: testAnimate },
        animationDuration: { r: false, t: testAnimationDuration },
        range: { r: true, t: testRange },
        orientation: { r: false, t: testOrientation },
        margin: { r: false, t: testMargin },
        limit: { r: false, t: testLimit },
        padding: { r: false, t: testPadding },
        behaviour: { r: true, t: testBehaviour },
        ariaFormat: { r: false, t: testAriaFormat },
        format: { r: false, t: testFormat },
        tooltips: { r: false, t: testTooltips },
        keyboardSupport: { r: true, t: testKeyboardSupport },
        documentElement: { r: false, t: testDocumentElement },
        cssPrefix: { r: true, t: testCssPrefix },
        cssClasses: { r: true, t: testCssClasses },
        handleAttributes: { r: false, t: testHandleAttributes },
    };

    const defaults = {
        connect: false,
        direction: "ltr",
        behaviour: "tap",
        orientation: "horizontal",
        keyboardSupport: true,
        cssPrefix: "noUi-",
        cssClasses: cssClasses,
        keyboardPageMultiplier: 5,
        keyboardMultiplier: 1,
        keyboardDefaultStep: 10,
    } as UpdatableOptions;

    // AriaFormat defaults to regular format, if any.
    if (options.format && !options.ariaFormat) {
        options.ariaFormat = options.format;
    }

    // Run all options through a testing mechanism to ensure correct
    // input. It should be noted that options might get modified to
    // be handled properly. E.g. wrapping integers in arrays.
    Object.keys(tests).forEach(function (name: OptionKey) {
        // If the option isn't set, but it is required, throw an error.
        if (!isSet(options[name]) && defaults[name] === undefined) {
            if ((tests[name] as any).r) {
                throw new Error("noUiSlider: '" + name + "' is required.");
            }

            return;
        }

        (tests[name] as any).t(parsed, !isSet(options[name]) ? defaults[name] : options[name]);
    });

    // Forward pips options
    parsed.pips = options.pips;

    // All recent browsers accept unprefixed transform.
    // We need -ms- for IE9 and -webkit- for older Android;
    // Assume use of -webkit- if unprefixed and -ms- are not supported.
    // https://caniuse.com/#feat=transforms2d
    const d = document.createElement("div");
    const msPrefix = (d.style as CSSStyleDeclarationIE10).msTransform !== undefined;
    const noPrefix = d.style.transform !== undefined;

    parsed.transformRule = noPrefix ? "transform" : msPrefix ? "msTransform" : "webkitTransform";

    // Pips don't move, so we can place them using left/top.
    const styles = [
        ["left", "top"],
        ["right", "bottom"],
    ];

    parsed.style = styles[parsed.dir][parsed.ort] as "left" | "top" | "right" | "bottom";

    return parsed;
}

//endregion

function scope(target: TargetElement, options: ParsedOptions, originalOptions: Options): API {
    const actions = getActions();
    const supportsTouchActionNone = getSupportsTouchActionNone();
    const supportsPassive = supportsTouchActionNone && getSupportsPassive();

    // All variables local to 'scope' are prefixed with 'scope_'

    // Slider DOM Nodes
    const scope_Target = target;
    let scope_Base: HTMLElement;
    let scope_Handles: HTMLElement[];
    let scope_Connects: (HTMLElement | false)[];
    let scope_Pips: HTMLElement | null;
    let scope_Tooltips: (HTMLElement | false)[] | null;

    // Slider state values
    let scope_Spectrum = options.spectrum;
    const scope_Values: number[] = [];
    let scope_Locations: number[] = [];
    const scope_HandleNumbers: number[] = [];
    let scope_ActiveHandlesCount = 0;
    const scope_Events: { [key: string]: EventCallback[] } = {};

    // Document Nodes
    const scope_Document = target.ownerDocument;
    const scope_DocumentElement = options.documentElement || scope_Document.documentElement;
    const scope_Body = scope_Document.body;

    // For horizontal sliders in standard ltr documents,
    // make .noUi-origin overflow to the left so the document doesn't scroll.
    const scope_DirOffset = scope_Document.dir === "rtl" || options.ort === 1 ? 0 : 100;

    // Creates a node, adds it to target, returns the new node.
    function addNodeTo(addTarget: HTMLElement, className: string | false): HTMLElement {
        const div = scope_Document.createElement("div");

        if (className) {
            addClass(div, className);
        }

        addTarget.appendChild(div);

        return div;
    }

    // Append a origin to the base
    function addOrigin(base: HTMLElement, handleNumber: number): HTMLElement {
        const origin = addNodeTo(base, options.cssClasses.origin);
        const handle = addNodeTo(origin, options.cssClasses.handle);

        addNodeTo(handle, options.cssClasses.touchArea);

        handle.setAttribute("data-handle", String(handleNumber));

        if (options.keyboardSupport) {
            // https://developer.mozilla.org/en-US/docs/Web/HTML/Global_attributes/tabindex
            // 0 = focusable and reachable
            handle.setAttribute("tabindex", "0");
            handle.addEventListener("keydown", function (event) {
                return eventKeydown(event, handleNumber);
            });
        }

        if (options.handleAttributes !== undefined) {
            const attributes: HandleAttributes = options.handleAttributes[handleNumber];
            Object.keys(attributes).forEach(function (attribute: string) {
                handle.setAttribute(attribute, attributes[attribute]);
            });
        }

        handle.setAttribute("role", "slider");
        handle.setAttribute("aria-orientation", options.ort ? "vertical" : "horizontal");

        if (handleNumber === 0) {
            addClass(handle, options.cssClasses.handleLower);
        } else if (handleNumber === options.handles - 1) {
            addClass(handle, options.cssClasses.handleUpper);
        }

        return origin;
    }

    // Insert nodes for connect elements
    function addConnect(base: HTMLElement, add: boolean): HTMLElement | false {
        if (!add) {
            return false;
        }

        return addNodeTo(base, options.cssClasses.connect);
    }

    // Add handles to the slider base.
    function addElements(connectOptions: boolean[], base: HTMLElement): void {
        const connectBase = addNodeTo(base, options.cssClasses.connects);

        scope_Handles = [];
        scope_Connects = [];

        scope_Connects.push(addConnect(connectBase, connectOptions[0]));

        // [::::O====O====O====]
        // connectOptions = [0, 1, 1, 1]

        for (let i = 0; i < options.handles; i++) {
            // Keep a list of all added handles.
            scope_Handles.push(addOrigin(base, i));
            scope_HandleNumbers[i] = i;
            scope_Connects.push(addConnect(connectBase, connectOptions[i + 1]));
        }
    }

    // Initialize a single slider.
    function addSlider(addTarget: HTMLElement): HTMLElement {
        // Apply classes and data to the target.
        addClass(addTarget, options.cssClasses.target);

        if (options.dir === 0) {
            addClass(addTarget, options.cssClasses.ltr);
        } else {
            addClass(addTarget, options.cssClasses.rtl);
        }

        if (options.ort === 0) {
            addClass(addTarget, options.cssClasses.horizontal);
        } else {
            addClass(addTarget, options.cssClasses.vertical);
        }

        const textDirection = getComputedStyle(addTarget).direction;

        if (textDirection === "rtl") {
            addClass(addTarget, options.cssClasses.textDirectionRtl);
        } else {
            addClass(addTarget, options.cssClasses.textDirectionLtr);
        }

        return addNodeTo(addTarget, options.cssClasses.base);
    }

    function addTooltip(handle: HTMLElement, handleNumber: number): HTMLElement | false {
        if (!options.tooltips || !options.tooltips[handleNumber]) {
            return false;
        }

        return addNodeTo(handle.firstChild as HTMLElement, options.cssClasses.tooltip);
    }

    function isSliderDisabled(): boolean {
        return scope_Target.hasAttribute("disabled");
    }

    // Disable the slider dragging if any handle is disabled
    function isHandleDisabled(handleNumber: number): boolean {
        const handleOrigin = scope_Handles[handleNumber];
        return handleOrigin.hasAttribute("disabled");
    }

    function removeTooltips(): void {
        if (scope_Tooltips) {
            removeEvent("update" + INTERNAL_EVENT_NS.tooltips);
            scope_Tooltips.forEach(function (tooltip) {
                if (tooltip) {
                    removeElement(tooltip);
                }
            });
            scope_Tooltips = null;
        }
    }

    // The tooltips option is a shorthand for using the 'update' event.
    function tooltips(): void {
        removeTooltips();

        // Tooltips are added with options.tooltips in original order.
        scope_Tooltips = scope_Handles.map(addTooltip);

        bindEvent("update" + INTERNAL_EVENT_NS.tooltips, function (values, handleNumber, unencoded) {
            if (!scope_Tooltips || !options.tooltips) {
                return;
            }

            if (scope_Tooltips[handleNumber] === false) {
                return;
            }

            let formattedValue = values[handleNumber];

            if (options.tooltips[handleNumber] !== true) {
                formattedValue = (<Formatter>options.tooltips[handleNumber]).to(unencoded[handleNumber]);
            }

            (<HTMLElement>scope_Tooltips[handleNumber]).innerHTML = <string>formattedValue;
        });
    }

    function aria(): void {
        removeEvent("update" + INTERNAL_EVENT_NS.aria);
        bindEvent("update" + INTERNAL_EVENT_NS.aria, function (values, handleNumber, unencoded, tap, positions) {
            // Update Aria Values for all handles, as a change in one changes min and max values for the next.
            scope_HandleNumbers.forEach(function (index) {
                const handle = scope_Handles[index];

                let min: number | string = <number>checkHandlePosition(scope_Locations, index, 0, true, true, true);
                let max: number | string = <number>checkHandlePosition(scope_Locations, index, 100, true, true, true);

                let now: number | string = positions[index];

                // Formatted value for display
                const text = String(options.ariaFormat.to(unencoded[index]));

                // Map to slider range values
                min = <string>scope_Spectrum.fromStepping(min).toFixed(1);
                max = <string>scope_Spectrum.fromStepping(max).toFixed(1);
                now = <string>scope_Spectrum.fromStepping(now).toFixed(1);

                handle.children[0].setAttribute("aria-valuemin", min);
                handle.children[0].setAttribute("aria-valuemax", max);
                handle.children[0].setAttribute("aria-valuenow", now);
                handle.children[0].setAttribute("aria-valuetext", text);
            });
        });
    }

    function getGroup(pips: Pips): number[] {
        // Use the range.
        if (pips.mode === PipsMode.Range || pips.mode === PipsMode.Steps) {
            return scope_Spectrum.xVal;
        }

        if (pips.mode === PipsMode.Count) {
            if (pips.values < 2) {
                throw new Error("noUiSlider: 'values' (>= 2) required for mode 'count'.");
            }

            // Divide 0 - 100 in 'count' parts.
            let interval = pips.values - 1;
            const spread = 100 / interval;

            const values = [];

            // MailList these parts and have them handled as 'positions'.
            while (interval--) {
                values[interval] = interval * spread;
            }

            values.push(100);

            return mapToRange(values, pips.stepped);
        }

        if (pips.mode === PipsMode.Positions) {
            // Map all percentages to on-range values.
            return mapToRange(pips.values, pips.stepped);
        }

        if (pips.mode === PipsMode.Values) {
            // If the value must be stepped, it needs to be converted to a percentage first.
            if (pips.stepped) {
                return pips.values.map(function (value: number) {
                    // Convert to percentage, apply step, return to value.
                    return scope_Spectrum.fromStepping(scope_Spectrum.getStep(scope_Spectrum.toStepping(value)));
                });
            }

            // Otherwise, we can simply use the values.
            return pips.values;
        }

        return []; // pips.mode = never
    }

    function mapToRange(values: number[], stepped: boolean | undefined): number[] {
        return values.map(function (value: number) {
            return scope_Spectrum.fromStepping(stepped ? scope_Spectrum.getStep(value) : value);
        });
    }

    function generateSpread(pips: Pips): { [key: string]: [number, PipsType] } {
        function safeIncrement(value: number, increment: number) {
            // Avoid floating point variance by dropping the smallest decimal places.
            return Number((value + increment).toFixed(7));
        }

        let group = getGroup(pips);
        const indexes: { [key: string]: [number, PipsType] } = {};
        const firstInRange = scope_Spectrum.xVal[0];
        const lastInRange = scope_Spectrum.xVal[scope_Spectrum.xVal.length - 1];
        let ignoreFirst = false;
        let ignoreLast = false;
        let prevPct = 0;

        // Create a copy of the group, sort it and filter away all duplicates.
        group = unique(
            group.slice().sort(function (a, b) {
                return a - b;
            })
        );

        // Make sure the range starts with the first element.
        if (group[0] !== firstInRange) {
            group.unshift(firstInRange);
            ignoreFirst = true;
        }

        // Likewise for the last one.
        if (group[group.length - 1] !== lastInRange) {
            group.push(lastInRange);
            ignoreLast = true;
        }

        group.forEach(function (current, index) {
            // Get the current step and the lower + upper positions.
            let step;
            let i;
            let q;
            const low = current;
            let high = group[index + 1];
            let newPct;
            let pctDifference;
            let pctPos;
            let type;
            let steps;
            let realSteps;
            let stepSize;
            const isSteps = pips.mode === PipsMode.Steps;

            // When using 'steps' mode, use the provided steps.
            // Otherwise, we'll step on to the next subrange.
            if (isSteps) {
                step = scope_Spectrum.xNumSteps[index];
            }

            // Default to a 'full' step.
            if (!step) {
                step = high - low;
            }

            // If high is undefined we are at the last subrange. Make sure it iterates once (#1088)
            if (high === undefined) {
                high = low;
            }

            // Make sure step isn't 0, which would cause an infinite loop (#654)
            step = Math.max(step, 0.0000001);

            // Find all steps in the subrange.
            for (i = low; i <= high; i = safeIncrement(i, step)) {
                // Get the percentage value for the current step,
                // calculate the size for the subrange.
                newPct = scope_Spectrum.toStepping(i);
                pctDifference = newPct - prevPct;

                steps = pctDifference / (pips.density || 1);
                realSteps = Math.round(steps);

                // This ratio represents the amount of percentage-space a point indicates.
                // For a density 1 the points/percentage = 1. For density 2, that percentage needs to be re-divided.
                // Round the percentage offset to an even number, then divide by two
                // to spread the offset on both sides of the range.
                stepSize = pctDifference / realSteps;

                // Divide all points evenly, adding the correct number to this subrange.
                // Run up to <= so that 100% gets a point, event if ignoreLast is set.
                for (q = 1; q <= realSteps; q += 1) {
                    // The ratio between the rounded value and the actual size might be ~1% off.
                    // Correct the percentage offset by the number of points
                    // per subrange. density = 1 will result in 100 points on the
                    // full range, 2 for 50, 4 for 25, etc.
                    pctPos = prevPct + q * stepSize;
                    indexes[pctPos.toFixed(5)] = [scope_Spectrum.fromStepping(pctPos), 0];
                }

                // Determine the point type.
                type = group.indexOf(i) > -1 ? PipsType.LargeValue : isSteps ? PipsType.SmallValue : PipsType.NoValue;

                // Enforce the 'ignoreFirst' option by overwriting the type for 0.
                if (!index && ignoreFirst && i !== high) {
                    type = 0;
                }

                if (!(i === high && ignoreLast)) {
                    // Mark the 'type' of this point. 0 = plain, 1 = real value, 2 = step value.
                    indexes[newPct.toFixed(5)] = [i, type];
                }

                // Update the percentage count.
                prevPct = newPct;
            }
        });

        return indexes;
    }

    function addMarking(
        spread: { [key: string]: [number, PipsType] },
        filterFunc: PipsFilter | undefined,
        formatter: PartialFormatter
    ): HTMLElement {
        const element = scope_Document.createElement("div");

        const valueSizeClasses: Record<PipsType, string> = {
            [PipsType.None]: "",
            [PipsType.NoValue]: options.cssClasses.valueNormal,
            [PipsType.LargeValue]: options.cssClasses.valueLarge,
            [PipsType.SmallValue]: options.cssClasses.valueSub,
        };
        const markerSizeClasses: Record<PipsType, string> = {
            [PipsType.None]: "",
            [PipsType.NoValue]: options.cssClasses.markerNormal,
            [PipsType.LargeValue]: options.cssClasses.markerLarge,
            [PipsType.SmallValue]: options.cssClasses.markerSub,
        };

        const valueOrientationClasses = [options.cssClasses.valueHorizontal, options.cssClasses.valueVertical];
        const markerOrientationClasses = [options.cssClasses.markerHorizontal, options.cssClasses.markerVertical];

        addClass(element, options.cssClasses.pips);
        addClass(element, options.ort === 0 ? options.cssClasses.pipsHorizontal : options.cssClasses.pipsVertical);

        function getClasses(type: PipsType, source: string) {
            const a = source === options.cssClasses.value;
            const orientationClasses = a ? valueOrientationClasses : markerOrientationClasses;
            const sizeClasses = a ? valueSizeClasses : markerSizeClasses;

            return source + " " + orientationClasses[options.ort] + " " + sizeClasses[type];
        }

        function addSpread(offset: string, value: number, type: PipsType) {
            // Apply the filter function, if it is set.
            type = filterFunc ? filterFunc(value, type) : type;

            if (type === PipsType.None) {
                return;
            }

            // Add a marker for every point
            let node = addNodeTo(element, false);
            node.className = getClasses(type, options.cssClasses.marker);
            node.style[options.style] = offset + "%";

            // Values are only appended for points marked '1' or '2'.
            if (type > PipsType.NoValue) {
                node = addNodeTo(element, false);
                node.className = getClasses(type, options.cssClasses.value);
                node.setAttribute("data-value", String(value));
                node.style[options.style] = offset + "%";
                node.innerHTML = String(formatter.to(value));
            }
        }

        // Append all points.
        Object.keys(spread).forEach(function (offset: string) {
            addSpread(offset, spread[offset][0], spread[offset][1]);
        });

        return element;
    }

    function removePips(): void {
        if (scope_Pips) {
            removeElement(scope_Pips);
            scope_Pips = null;
        }
    }

    function pips(pips: Pips): HTMLElement {
        // Fix #669
        removePips();

        const spread = generateSpread(pips);
        const filter = pips.filter;
        const format: PartialFormatter = pips.format || {
            to: function (value) {
                return String(Math.round(value));
            },
        };

        scope_Pips = scope_Target.appendChild(addMarking(spread, filter, format));

        return scope_Pips;
    }

    // Shorthand for base dimensions.
    function baseSize(): number {
        const rect = scope_Base.getBoundingClientRect();
        const alt = ("offset" + ["Width", "Height"][options.ort]) as "offsetWidth" | "offsetHeight";
        return options.ort === 0 ? rect.width || scope_Base[alt] : rect.height || scope_Base[alt];
    }

    // Handler for attaching events trough a proxy.
    function attachEvent(
        events: string,
        element: HTMLElement,
        callback: (event: BrowserEvent, data: EventData) => void,
        data: EventData
    ): [string, EventHandler][] {
        // This function can be used to 'filter' events to the slider.
        // element is a node, not a nodeList

        const method: EventHandler = function (event: BrowserEvent): false | undefined {
            const e = fixEvent(event, data.pageOffset, data.target || element);

            // fixEvent returns false if this event has a different target
            // when handling (multi-) touch events;
            if (!e) {
                return false;
            }

            // doNotReject is passed by all end events to make sure released touches
            // are not rejected, leaving the slider "stuck" to the cursor;
            if (isSliderDisabled() && !data.doNotReject) {
                return false;
            }

            // Stop if an active 'tap' transition is taking place.
            if (hasClass(scope_Target, options.cssClasses.tap) && !data.doNotReject) {
                return false;
            }

            // Ignore right or middle clicks on start #454
            if (events === actions.start && e.buttons !== undefined && e.buttons > 1) {
                return false;
            }

            // Ignore right or middle clicks on start #454
            if (data.hover && e.buttons) {
                return false;
            }

            // 'supportsPassive' is only true if a browser also supports touch-action: none in CSS.
            // iOS safari does not, so it doesn't get to benefit from passive scrolling. iOS does support
            // touch-action: manipulation, but that allows panning, which breaks
            // sliders after zooming/on non-responsive pages.
            // See: https://bugs.webkit.org/show_bug.cgi?id=133112
            if (!supportsPassive) {
                e.preventDefault();
            }

            e.calcPoint = e.points[options.ort];

            // Call the event handler with the event [ and additional data ].
            callback(e, data);

            return;
        };

        const methods: [string, EventHandler][] = [];

        // Bind a closure on the target for every event type.
        events.split(" ").forEach(function (eventName: string): void {
            element.addEventListener(eventName, method, supportsPassive ? { passive: true } : false);
            methods.push([eventName, method]);
        });

        return methods;
    }

    // Provide a clean event with standardized offset values.
    function fixEvent(
        e: BrowserEvent,
        pageOffset: PageOffset | undefined,
        eventTarget: HTMLElement
    ): BrowserEvent | false {
        // Filter the event to register the type, which can be
        // touch, mouse or pointer. Offset changes need to be
        // made on an event specific basis.
        const touch = e.type.indexOf("touch") === 0;
        const mouse = e.type.indexOf("mouse") === 0;
        let pointer = e.type.indexOf("pointer") === 0;

        let x = 0;
        let y = 0;

        // IE10 implemented pointer events with a prefix;
        if (e.type.indexOf("MSPointer") === 0) {
            pointer = true;
        }

        // Erroneous events seem to be passed in occasionally on iOS/iPadOS after user finishes interacting with
        // the slider. They appear to be of type MouseEvent, yet they don't have usual properties set. Ignore
        // events that have no touches or buttons associated with them. (#1057, #1079, #1095)
        if (e.type === "mousedown" && !e.buttons && !e.touches) {
            return false;
        }

        // The only thing one handle should be concerned about is the touches that originated on top of it.
        if (touch) {
            // Returns true if a touch originated on the target.
            const isTouchOnTarget = function (checkTouch: Touch) {
                const target: HTMLElement = checkTouch.target as HTMLElement;

                return (
                    target === eventTarget ||
                    eventTarget.contains(target) ||
                    (e.composed && e.composedPath().shift() === eventTarget)
                );
            };

            // In the case of touchstart events, we need to make sure there is still no more than one
            // touch on the target so we look amongst all touches.
            if (e.type === "touchstart") {
                const targetTouches: Touch[] = Array.prototype.filter.call(e.touches, isTouchOnTarget);

                // Do not support more than one touch per handle.
                if (targetTouches.length > 1) {
                    return false;
                }

                x = targetTouches[0].pageX;
                y = targetTouches[0].pageY;
            } else {
                // In the other cases, find on changedTouches is enough.
                const targetTouch = (Array.prototype as any).find.call(e.changedTouches, isTouchOnTarget);

                // Cancel if the target touch has not moved.
                if (!targetTouch) {
                    return false;
                }

                x = targetTouch.pageX;
                y = targetTouch.pageY;
            }
        }

        pageOffset = pageOffset || getPageOffset(scope_Document);

        if (mouse || pointer) {
            x = e.clientX + pageOffset.x;
            y = e.clientY + pageOffset.y;
        }

        e.pageOffset = pageOffset;
        e.points = [x, y];
        e.cursor = mouse || pointer; // Fix #435

        return e;
    }

    // Translate a coordinate in the document to a percentage on the slider
    function calcPointToPercentage(calcPoint: number) {
        const location = calcPoint - offset(scope_Base, options.ort);
        let proposal = (location * 100) / baseSize();

        // Clamp proposal between 0% and 100%
        // Out-of-bound coordinates may occur when .noUi-base pseudo-elements
        // are used (e.g. contained handles feature)
        proposal = limit(proposal);

        return options.dir ? 100 - proposal : proposal;
    }

    // Find handle closest to a certain percentage on the slider
    function getClosestHandle(clickedPosition: number): number | false {
        let smallestDifference = 100;
        let handleNumber: number | false = false;

        scope_Handles.forEach(function (handle, index) {
            // Disabled handles are ignored
            if (isHandleDisabled(index)) {
                return;
            }

            const handlePosition = scope_Locations[index];
            const differenceWithThisHandle = Math.abs(handlePosition - clickedPosition);

            // Initial state
            const clickAtEdge = differenceWithThisHandle === 100 && smallestDifference === 100;

            // Difference with this handle is smaller than the previously checked handle
            const isCloser = differenceWithThisHandle < smallestDifference;
            const isCloserAfter = differenceWithThisHandle <= smallestDifference && clickedPosition > handlePosition;

            if (isCloser || isCloserAfter || clickAtEdge) {
                handleNumber = index;
                smallestDifference = differenceWithThisHandle;
            }
        });

        return handleNumber;
    }

    // Fire 'end' when a mouse or pen leaves the document.
    function documentLeave(event: BrowserEvent, data: EndEventData): void {
        if (
            event.type === "mouseout" &&
            (event.target as HTMLElement).nodeName === "HTML" &&
            event.relatedTarget === null
        ) {
            eventEnd(event, data);
        }
    }

    // Handle movement on document for handle and range drag.
    function eventMove(event: BrowserEvent, data: MoveEventData): void {
        // Fix #498
        // Check value of .buttons in 'start' to work around a bug in IE10 mobile (data.buttonsProperty).
        // https://connect.microsoft.com/IE/feedback/details/927005/mobile-ie10-windows-phone-buttons-property-of-pointermove-event-always-zero
        // IE9 has .buttons and .which zero on mousemove.
        // Firefox breaks the spec MDN defines.
        if (navigator.appVersion.indexOf("MSIE 9") === -1 && event.buttons === 0 && data.buttonsProperty !== 0) {
            return eventEnd(event, data);
        }

        // Check if we are moving up or down
        const movement = (options.dir ? -1 : 1) * (event.calcPoint - data.startCalcPoint);

        // Convert the movement into a percentage of the slider width/height
        const proposal = (movement * 100) / data.baseSize;

        moveHandles(movement > 0, proposal, data.locations, data.handleNumbers, data.connect);
    }

    // Unbind move events on document, call callbacks.
    function eventEnd(event: BrowserEvent, data: EndEventData): void {
        // The handle is no longer active, so remove the class.
        if (data.handle) {
            removeClass(data.handle, options.cssClasses.active);
            scope_ActiveHandlesCount -= 1;
        }

        // Unbind the move and end events, which are added on 'start'.
        data.listeners.forEach(function (c: [string, EventHandler]) {
            scope_DocumentElement.removeEventListener(c[0], c[1]);
        });

        if (scope_ActiveHandlesCount === 0) {
            // Remove dragging class.
            removeClass(scope_Target, options.cssClasses.drag);
            setZindex();

            // Remove cursor styles and text-selection events bound to the body.
            if (event.cursor) {
                scope_Body.style.cursor = "";
                scope_Body.removeEventListener("selectstart", preventDefault);
            }
        }

        if (options.events.smoothSteps) {
            data.handleNumbers.forEach(function (handleNumber) {
                setHandle(handleNumber, scope_Locations[handleNumber], true, true, false, false);
            });

            data.handleNumbers.forEach(function (handleNumber: number) {
                fireEvent("update", handleNumber);
            });
        }

        data.handleNumbers.forEach(function (handleNumber: number) {
            fireEvent("change", handleNumber);
            fireEvent("set", handleNumber);
            fireEvent("end", handleNumber);
        });
    }

    // Bind move events on document.
    function eventStart(event: BrowserEvent, data: EventData): void {
        // Ignore event if any handle is disabled
        if (data.handleNumbers.some(isHandleDisabled)) {
            return;
        }

        let handle;

        if (data.handleNumbers.length === 1) {
            const handleOrigin = scope_Handles[data.handleNumbers[0]];

            handle = handleOrigin.children[0] as HTMLElement;
            scope_ActiveHandlesCount += 1;

            // Mark the handle as 'active' so it can be styled.
            addClass(handle, options.cssClasses.active);
        }

        // A drag should never propagate up to the 'tap' event.
        event.stopPropagation();

        // Record the event listeners.
        const listeners: [string, EventHandler][] = [];

        // Attach the move and end events.
        const moveEvent = attachEvent(actions.move, scope_DocumentElement, eventMove, {
            // The event target has changed so we need to propagate the original one so that we keep
            // relying on it to extract target touches.
            target: event.target as HTMLElement,
            handle: handle,
            connect: data.connect,
            listeners: listeners,
            startCalcPoint: event.calcPoint,
            baseSize: baseSize(),
            pageOffset: event.pageOffset,
            handleNumbers: data.handleNumbers,
            buttonsProperty: event.buttons,
            locations: scope_Locations.slice(),
        });

        const endEvent = attachEvent(actions.end, scope_DocumentElement, eventEnd, {
            target: event.target as HTMLElement,
            handle: handle,
            listeners: listeners,
            doNotReject: true,
            handleNumbers: data.handleNumbers,
        });

        const outEvent = attachEvent("mouseout", scope_DocumentElement, documentLeave, {
            target: event.target as HTMLElement,
            handle: handle,
            listeners: listeners,
            doNotReject: true,
            handleNumbers: data.handleNumbers,
        });

        // We want to make sure we pushed the listeners in the listener list rather than creating
        // a new one as it has already been passed to the event handlers.
        listeners.push.apply(listeners, moveEvent.concat(endEvent, outEvent));

        // Text selection isn't an issue on touch devices,
        // so adding cursor styles can be skipped.
        if (event.cursor) {
            // Prevent the 'I' cursor and extend the range-drag cursor.
            scope_Body.style.cursor = getComputedStyle(event.target as Element).cursor;

            // Mark the target with a dragging state.
            if (scope_Handles.length > 1) {
                addClass(scope_Target, options.cssClasses.drag);
            }

            // Prevent text selection when dragging the handles.
            // In noUiSlider <= 9.2.0, this was handled by calling preventDefault on mouse/touch start/move,
            // which is scroll blocking. The selectstart event is supported by FireFox starting from version 52,
            // meaning the only holdout is iOS Safari. This doesn't matter: text selection isn't triggered there.
            // The 'cursor' flag is false.
            // See: http://caniuse.com/#search=selectstart
            scope_Body.addEventListener("selectstart", preventDefault, false);
        }

        data.handleNumbers.forEach(function (handleNumber: number) {
            fireEvent("start", handleNumber);
        });
    }

    // Move closest handle to tapped location.
    function eventTap(event: BrowserEvent): void {
        // The tap event shouldn't propagate up
        event.stopPropagation();

        const proposal = calcPointToPercentage(event.calcPoint);
        const handleNumber = getClosestHandle(proposal);

        // Tackle the case that all handles are 'disabled'.
        if (handleNumber === false) {
            return;
        }

        // Flag the slider as it is now in a transitional state.
        // Transition takes a configurable amount of ms (default 300). Re-enable the slider after that.
        if (!options.events.snap) {
            addClassFor(scope_Target, options.cssClasses.tap, options.animationDuration);
        }

        setHandle(handleNumber, proposal, true, true);

        setZindex();

        fireEvent("slide", handleNumber, true);
        fireEvent("update", handleNumber, true);

        if (!options.events.snap) {
            fireEvent("change", handleNumber, true);
            fireEvent("set", handleNumber, true);
        } else {
            eventStart(event, { handleNumbers: [handleNumber] });
        }
    }

    // Fires a 'hover' event for a hovered mouse/pen position.
    function eventHover(event: BrowserEvent): void {
        const proposal = calcPointToPercentage(event.calcPoint);

        const to = scope_Spectrum.getStep(proposal);
        const value = scope_Spectrum.fromStepping(to);

        Object.keys(scope_Events).forEach(function (targetEvent: string) {
            if ("hover" === targetEvent.split(".")[0]) {
                scope_Events[targetEvent].forEach(function (callback) {
                    callback.call(scope_Self, value);
                });
            }
        });
    }

    // Handles keydown on focused handles
    // Don't move the document when pressing arrow keys on focused handles
    function eventKeydown(event: KeyboardEvent, handleNumber: number): boolean {
        if (isSliderDisabled() || isHandleDisabled(handleNumber)) {
            return false;
        }

        const horizontalKeys = ["Left", "Right"];
        const verticalKeys = ["Down", "Up"];
        const largeStepKeys = ["PageDown", "PageUp"];
        const edgeKeys = ["Home", "End"];

        if (options.dir && !options.ort) {
            // On an right-to-left slider, the left and right keys act inverted
            horizontalKeys.reverse();
        } else if (options.ort && !options.dir) {
            // On a top-to-bottom slider, the up and down keys act inverted
            verticalKeys.reverse();
            largeStepKeys.reverse();
        }

        // Strip "Arrow" for IE compatibility. https://developer.mozilla.org/en-US/docs/Web/API/KeyboardEvent/key
        const key = event.key.replace("Arrow", "");

        const isLargeDown = key === largeStepKeys[0];
        const isLargeUp = key === largeStepKeys[1];
        const isDown = key === verticalKeys[0] || key === horizontalKeys[0] || isLargeDown;
        const isUp = key === verticalKeys[1] || key === horizontalKeys[1] || isLargeUp;
        const isMin = key === edgeKeys[0];
        const isMax = key === edgeKeys[1];

        if (!isDown && !isUp && !isMin && !isMax) {
            return true;
        }

        event.preventDefault();

        let to;

        if (isUp || isDown) {
            const direction = isDown ? 0 : 1;
            const steps = getNextStepsForHandle(handleNumber);
            let step = steps[direction];

            // At the edge of a slider, do nothing
            if (step === null) {
                return false;
            }

            // No step set, use the default of 10% of the sub-range
            if (step === false) {
                step = scope_Spectrum.getDefaultStep(
                    scope_Locations[handleNumber],
                    isDown,
                    options.keyboardDefaultStep
                ) as number;
            }

            if (isLargeUp || isLargeDown) {
                step *= options.keyboardPageMultiplier;
            } else {
                step *= options.keyboardMultiplier;
            }

            // Step over zero-length ranges (#948);
            step = Math.max(step, 0.0000001);

            // Decrement for down steps
            step = (isDown ? -1 : 1) * step;

            to = scope_Values[handleNumber] + step;
        } else if (isMax) {
            // End key
            to = options.spectrum.xVal[options.spectrum.xVal.length - 1];
        } else {
            // Home key
            to = options.spectrum.xVal[0];
        }

        setHandle(handleNumber, scope_Spectrum.toStepping(to), true, true);

        fireEvent("slide", handleNumber);
        fireEvent("update", handleNumber);
        fireEvent("change", handleNumber);
        fireEvent("set", handleNumber);

        return false;
    }

    // Attach events to several slider parts.
    function bindSliderEvents(behaviour: Behaviour) {
        // Attach the standard drag event to the handles.
        if (!behaviour.fixed) {
            scope_Handles.forEach(function (handle, index) {
                // These events are only bound to the visual handle
                // element, not the 'real' origin element.
                attachEvent(actions.start, handle.children[0] as HTMLElement, eventStart, {
                    handleNumbers: [index],
                });
            });
        }

        // Attach the tap event to the slider base.
        if (behaviour.tap) {
            attachEvent(actions.start, scope_Base, eventTap, {} as EventData);
        }

        // Fire hover events
        if (behaviour.hover) {
            attachEvent(actions.move, scope_Base, eventHover, {
                hover: true,
            } as EventData);
        }

        // Make the range draggable.
        if (behaviour.drag) {
            scope_Connects.forEach(function (connect, index) {
                if (connect === false || index === 0 || index === scope_Connects.length - 1) {
                    return;
                }

                const handleBefore = scope_Handles[index - 1];
                const handleAfter = scope_Handles[index];
                const eventHolders = [connect];

                let handlesToDrag = [handleBefore, handleAfter];
                let handleNumbersToDrag = [index - 1, index];

                addClass(connect, options.cssClasses.draggable);

                // When the range is fixed, the entire range can
                // be dragged by the handles. The handle in the first
                // origin will propagate the start event upward,
                // but it needs to be bound manually on the other.
                if (behaviour.fixed) {
                    eventHolders.push(handleBefore.children[0] as HTMLElement);
                    eventHolders.push(handleAfter.children[0] as HTMLElement);
                }

                if (behaviour.dragAll) {
                    handlesToDrag = scope_Handles;
                    handleNumbersToDrag = scope_HandleNumbers;
                }

                eventHolders.forEach(function (eventHolder) {
                    attachEvent(actions.start, eventHolder, eventStart, {
                        handles: handlesToDrag,
                        handleNumbers: handleNumbersToDrag,
                        connect: connect,
                    });
                });
            });
        }
    }

    // Attach an event to this slider, possibly including a namespace
    function bindEvent(namespacedEvent: string, callback: EventCallback): void {
        scope_Events[namespacedEvent] = scope_Events[namespacedEvent] || [];
        scope_Events[namespacedEvent].push(callback);

        // If the event bound is 'update,' fire it immediately for all handles.
        if (namespacedEvent.split(".")[0] === "update") {
            scope_Handles.forEach(function (a, index) {
                fireEvent("update", index);
            });
        }
    }

    function isInternalNamespace(namespace: string): boolean {
        return namespace === INTERNAL_EVENT_NS.aria || namespace === INTERNAL_EVENT_NS.tooltips;
    }

    // Undo attachment of event
    function removeEvent(namespacedEvent: string): void {
        const event = namespacedEvent && namespacedEvent.split(".")[0];
        const namespace = event ? namespacedEvent.substring(event.length) : namespacedEvent;

        Object.keys(scope_Events).forEach(function (bind) {
            const tEvent = bind.split(".")[0];
            const tNamespace = bind.substring(tEvent.length);
            if ((!event || event === tEvent) && (!namespace || namespace === tNamespace)) {
                // only delete protected internal event if intentional
                if (!isInternalNamespace(tNamespace) || namespace === tNamespace) {
                    delete scope_Events[bind];
                }
            }
        });
    }

    // External event handling
    function fireEvent(eventName: string, handleNumber: number, tap?: boolean): void {
        Object.keys(scope_Events).forEach(function (targetEvent: string): void {
            const eventType = targetEvent.split(".")[0];

            if (eventName === eventType) {
                scope_Events[targetEvent].forEach(function (callback) {
                    callback.call(
                        // Use the slider public API as the scope ('this')
                        scope_Self,
                        // Return values as array, so arg_1[arg_2] is always valid.
                        scope_Values.map(options.format.to),
                        // Handle index, 0 or 1
                        handleNumber,
                        // Un-formatted slider values
                        scope_Values.slice(),
                        // Event is fired by tap, true or false
                        tap || false,
                        // Left offset of the handle, in relation to the slider
                        scope_Locations.slice(),
                        // add the slider public API to an accessible parameter when this is unavailable
                        scope_Self
                    );
                });
            }
        });
    }

    // Split out the handle positioning logic so the Move event can use it, too
    function checkHandlePosition(
        reference: number[],
        handleNumber: number,
        to: number,
        lookBackward: boolean,
        lookForward: boolean,
        getValue: boolean,
        smoothSteps?: boolean
    ): number | false {
        let distance;

        // For sliders with multiple handles, limit movement to the other handle.
        // Apply the margin option by adding it to the handle positions.
        if (scope_Handles.length > 1 && !options.events.unconstrained) {
            if (lookBackward && handleNumber > 0) {
                distance = scope_Spectrum.getAbsoluteDistance(reference[handleNumber - 1], options.margin, false);
                to = Math.max(to, distance);
            }

            if (lookForward && handleNumber < scope_Handles.length - 1) {
                distance = scope_Spectrum.getAbsoluteDistance(reference[handleNumber + 1], options.margin, true);
                to = Math.min(to, distance);
            }
        }

        // The limit option has the opposite effect, limiting handles to a
        // maximum distance from another. Limit must be > 0, as otherwise
        // handles would be unmovable.
        if (scope_Handles.length > 1 && options.limit) {
            if (lookBackward && handleNumber > 0) {
                distance = scope_Spectrum.getAbsoluteDistance(reference[handleNumber - 1], options.limit, false);
                to = Math.min(to, distance);
            }

            if (lookForward && handleNumber < scope_Handles.length - 1) {
                distance = scope_Spectrum.getAbsoluteDistance(reference[handleNumber + 1], options.limit, true);
                to = Math.max(to, distance);
            }
        }

        // The padding option keeps the handles a certain distance from the
        // edges of the slider. Padding must be > 0.
        if (options.padding) {
            if (handleNumber === 0) {
                distance = scope_Spectrum.getAbsoluteDistance(0, options.padding[0], false);
                to = Math.max(to, distance);
            }

            if (handleNumber === scope_Handles.length - 1) {
                distance = scope_Spectrum.getAbsoluteDistance(100, options.padding[1], true);
                to = Math.min(to, distance);
            }
        }

        if (!smoothSteps) {
            to = scope_Spectrum.getStep(to);
        }

        // Limit percentage to the 0 - 100 range
        to = limit(to);

        // Return false if handle can't move
        if (to === reference[handleNumber] && !getValue) {
            return false;
        }

        return to;
    }

    // Uses slider orientation to create CSS rules. a = base value;
    function inRuleOrder(v: string | number, a: string | number): string {
        const o = options.ort;
        return (o ? a : v) + ", " + (o ? v : a);
    }

    // Moves handle(s) by a percentage
    // (bool, % to move, [% where handle started, ...], [index in scope_Handles, ...])
    function moveHandles(
        upward: boolean,
        proposal: number,
        locations: number[],
        handleNumbers: number[],
        connect?: HTMLElement
    ): void {
        const proposals = locations.slice();

        // Store first handle now, so we still have it in case handleNumbers is reversed
        const firstHandle = handleNumbers[0];

        const smoothSteps = options.events.smoothSteps;

        let b = [!upward, upward];
        let f = [upward, !upward];

        // Copy handleNumbers so we don't change the dataset
        handleNumbers = handleNumbers.slice();

        // Check to see which handle is 'leading'.
        // If that one can't move the second can't either.
        if (upward) {
            handleNumbers.reverse();
        }

        // Step 1: get the maximum percentage that any of the handles can move
        if (handleNumbers.length > 1) {
            handleNumbers.forEach(function (handleNumber, o) {
                const to = checkHandlePosition(
                    proposals,
                    handleNumber,
                    proposals[handleNumber] + proposal,
                    b[o],
                    f[o],
                    false,
                    smoothSteps
                );

                // Stop if one of the handles can't move.
                if (to === false) {
                    proposal = 0;
                } else {
                    proposal = to - proposals[handleNumber];
                    proposals[handleNumber] = to;
                }
            });
        }

        // If using one handle, check backward AND forward
        else {
            b = f = [true];
        }

        let state = false;

        // Step 2: Try to set the handles with the found percentage
        handleNumbers.forEach(function (handleNumber, o) {
            state =
                setHandle(handleNumber, locations[handleNumber] + proposal, b[o], f[o], false, smoothSteps) || state;
        });

        // Step 3: If a handle moved, fire events
        if (state) {
            handleNumbers.forEach(function (handleNumber) {
                fireEvent("update", handleNumber);
                fireEvent("slide", handleNumber);
            });

            // If target is a connect, then fire drag event
            if (connect != undefined) {
                fireEvent("drag", firstHandle);
            }
        }
    }

    // Takes a base value and an offset. This offset is used for the connect bar size.
    // In the initial design for this feature, the origin element was 1% wide.
    // Unfortunately, a rounding bug in Chrome makes it impossible to implement this feature
    // in this manner: https://bugs.chromium.org/p/chromium/issues/detail?id=798223
    function transformDirection(a: number, b: number): number {
        return options.dir ? 100 - a - b : a;
    }

    // Updates scope_Locations and scope_Values, updates visual state
    function updateHandlePosition(handleNumber: number, to: number): void {
        // Update locations.
        scope_Locations[handleNumber] = to;

        // Convert the value to the slider stepping/range.
        scope_Values[handleNumber] = scope_Spectrum.fromStepping(to);

        const translation = transformDirection(to, 0) - scope_DirOffset;
        const translateRule = "translate(" + inRuleOrder(translation + "%", "0") + ")";

        (scope_Handles[handleNumber].style as CSSStyleDeclarationIE10)[options.transformRule] = translateRule;

        updateConnect(handleNumber);
        updateConnect(handleNumber + 1);
    }

    // Handles before the slider middle are stacked later = higher,
    // Handles after the middle later is lower
    // [[7] [8] .......... | .......... [5] [4]
    function setZindex(): void {
        scope_HandleNumbers.forEach(function (handleNumber: number): void {
            const dir = scope_Locations[handleNumber] > 50 ? -1 : 1;
            const zIndex = 3 + (scope_Handles.length + dir * handleNumber);
            scope_Handles[handleNumber].style.zIndex = String(zIndex);
        });
    }

    // Test suggested values and apply margin, step.
    // if exactInput is true, don't run checkHandlePosition, then the handle can be placed in between steps (#436)
    function setHandle(
        handleNumber: number,
        to: number | false,
        lookBackward: boolean,
        lookForward: boolean,
        exactInput?: boolean,
        smoothSteps?: boolean
    ): boolean {
        if (!exactInput) {
            to = checkHandlePosition(
                scope_Locations,
                handleNumber,
                <number>to,
                lookBackward,
                lookForward,
                false,
                smoothSteps
            );
        }

        if (to === false) {
            return false;
        }

        updateHandlePosition(handleNumber, to);

        return true;
    }

    // Updates style attribute for connect nodes
    function updateConnect(index: number): void {
        // Skip connects set to false
        if (!scope_Connects[index]) {
            return;
        }

        let l = 0;
        let h = 100;

        if (index !== 0) {
            l = scope_Locations[index - 1];
        }

        if (index !== scope_Connects.length - 1) {
            h = scope_Locations[index];
        }

        // We use two rules:
        // 'translate' to change the left/top offset;
        // 'scale' to change the width of the element;
        // As the element has a width of 100%, a translation of 100% is equal to 100% of the parent (.noUi-base)
        const connectWidth = h - l;
        const translateRule = "translate(" + inRuleOrder(transformDirection(l, connectWidth) + "%", "0") + ")";
        const scaleRule = "scale(" + inRuleOrder(connectWidth / 100, "1") + ")";

        ((<HTMLElement>scope_Connects[index]).style as CSSStyleDeclarationIE10)[options.transformRule] =
            translateRule + " " + scaleRule;
    }

    // Parses value passed to .set method. Returns current value if not parse-able.
    function resolveToValue(to: null | false | undefined | string | number, handleNumber: number): number {
        // Setting with null indicates an 'ignore'.
        // Inputting 'false' is invalid.
        if (to === null || to === false || to === undefined) {
            return scope_Locations[handleNumber];
        }

        // If a formatted number was passed, attempt to decode it.
        if (typeof to === "number") {
            to = String(to);
        }

        to = options.format.from(to);

        if (to !== false) {
            to = scope_Spectrum.toStepping(to);
        }

        // If parsing the number failed, use the current value.
        if (to === false || isNaN(to)) {
            return scope_Locations[handleNumber];
        }

        return to;
    }

    // Set the slider value.
    function valueSet(input: StartValues, fireSetEvent?: boolean, exactInput?: boolean): void {
        const values = asArray(input);
        const isInit = scope_Locations[0] === undefined;

        // Event fires by default
        fireSetEvent = fireSetEvent === undefined ? true : fireSetEvent;

        // Animation is optional.
        // Make sure the initial values were set before using animated placement.
        if (options.animate && !isInit) {
            addClassFor(scope_Target, options.cssClasses.tap, options.animationDuration);
        }

        // First pass, without lookAhead but with lookBackward. Values are set from left to right.
        scope_HandleNumbers.forEach(function (handleNumber) {
            setHandle(handleNumber, resolveToValue(values[handleNumber], handleNumber), true, false, exactInput);
        });

        let i = scope_HandleNumbers.length === 1 ? 0 : 1;

        // Spread handles evenly across the slider if the range has no size (min=max)
        if (isInit && scope_Spectrum.hasNoSize()) {
            exactInput = true;

            scope_Locations[0] = 0;

            if (scope_HandleNumbers.length > 1) {
                const space = 100 / (scope_HandleNumbers.length - 1);

                scope_HandleNumbers.forEach(function (handleNumber) {
                    scope_Locations[handleNumber] = handleNumber * space;
                });
            }
        }

        // Secondary passes. Now that all base values are set, apply constraints.
        // Iterate all handles to ensure constraints are applied for the entire slider (Issue #1009)
        for (; i < scope_HandleNumbers.length; ++i) {
            scope_HandleNumbers.forEach(function (handleNumber) {
                setHandle(handleNumber, scope_Locations[handleNumber], true, true, exactInput);
            });
        }

        setZindex();

        scope_HandleNumbers.forEach(function (handleNumber) {
            fireEvent("update", handleNumber);

            // Fire the event only for handles that received a new value, as per #579
            if (values[handleNumber] !== null && fireSetEvent) {
                fireEvent("set", handleNumber);
            }
        });
    }

    // Reset slider to initial values
    function valueReset(fireSetEvent?: boolean): void {
        valueSet(options.start, fireSetEvent);
    }

    // Set value for a single handle
    function valueSetHandle(
        handleNumber: number,
        value: string | number,
        fireSetEvent?: boolean,
        exactInput?: boolean
    ): void {
        // Ensure numeric input
        handleNumber = Number(handleNumber);

        if (!(handleNumber >= 0 && handleNumber < scope_HandleNumbers.length)) {
            throw new Error("noUiSlider: invalid handle number, got: " + handleNumber);
        }

        // Look both backward and forward, since we don't want this handle to "push" other handles (#960);
        // The exactInput argument can be used to ignore slider stepping (#436)
        setHandle(handleNumber, resolveToValue(value, handleNumber), true, true, exactInput);

        fireEvent("update", handleNumber);

        if (fireSetEvent) {
            fireEvent("set", handleNumber);
        }
    }

    // Get the slider value.
    function valueGet(unencoded = false): GetResult {
        if (unencoded) {
            // return a copy of the raw values
            return scope_Values.length === 1 ? scope_Values[0] : scope_Values.slice(0);
        }
        const values = scope_Values.map(options.format.to);

        // If only one handle is used, return a single value.
        if (values.length === 1) {
            return values[0];
        }

        return values;
    }

    // Removes classes from the root and empties it.
    function destroy(): void {
        // remove protected internal listeners
        removeEvent(INTERNAL_EVENT_NS.aria);
        removeEvent(INTERNAL_EVENT_NS.tooltips);

        Object.keys(options.cssClasses).forEach((key: keyof CssClasses) => {
            removeClass(scope_Target, options.cssClasses[key]);
        });

        while (scope_Target.firstChild) {
            scope_Target.removeChild(scope_Target.firstChild);
        }

        delete scope_Target.noUiSlider;
    }

    function getNextStepsForHandle(handleNumber: number): NextStepsForHandle {
        const location = scope_Locations[handleNumber];
        const nearbySteps = scope_Spectrum.getNearbySteps(location);
        const value: number = scope_Values[handleNumber];
        let increment: number | false | null = nearbySteps.thisStep.step;
        let decrement: number | false | null = null;

        // If snapped, directly use defined step value
        if (options.snap) {
            return [
                value - nearbySteps.stepBefore.startValue || null,
                nearbySteps.stepAfter.startValue - value || null,
            ];
        }

        // If the next value in this step moves into the next step,
        // the increment is the start of the next step - the current value
        if (increment !== false) {
            if (value + increment > nearbySteps.stepAfter.startValue) {
                increment = nearbySteps.stepAfter.startValue - value;
            }
        }

        // If the value is beyond the starting point
        if (value > nearbySteps.thisStep.startValue) {
            decrement = nearbySteps.thisStep.step;
        } else if (nearbySteps.stepBefore.step === false) {
            decrement = false;
        }

        // If a handle is at the start of a step, it always steps back into the previous step first
        else {
            decrement = value - nearbySteps.stepBefore.highestStep;
        }

        // Now, if at the slider edges, there is no in/decrement
        if (location === 100) {
            increment = null;
        } else if (location === 0) {
            decrement = null;
        }

        // As per #391, the comparison for the decrement step can have some rounding issues.
        const stepDecimals = scope_Spectrum.countStepDecimals();

        // Round per #391
        if (increment !== null && increment !== false) {
            increment = Number(increment.toFixed(stepDecimals));
        }

        if (decrement !== null && decrement !== false) {
            decrement = Number(decrement.toFixed(stepDecimals));
        }

        return [decrement, increment];
    }

    // Get the current step size for the slider.
    function getNextSteps(): NextStepsForHandle[] {
        return scope_HandleNumbers.map(getNextStepsForHandle);
    }

    // Updatable: margin, limit, padding, step, range, animate, snap
    function updateOptions(optionsToUpdate: UpdatableOptions, fireSetEvent: boolean): void {
        // Spectrum is created using the range, snap, direction and step options.
        // 'snap' and 'step' can be updated.
        // If 'snap' and 'step' are not passed, they should remain unchanged.
        const v = valueGet();

        const updateAble: OptionKey[] = [
            "margin",
            "limit",
            "padding",
            "range",
            "animate",
            "snap",
            "step",
            "format",
            "pips",
            "tooltips",
        ];

        // Only change options that we're actually passed to update.
        updateAble.forEach(function (name: OptionKey) {
            // Check for undefined. null removes the value.
            if (optionsToUpdate[name] !== undefined) {
                (originalOptions[name] as any) = optionsToUpdate[name];
            }
        });

        const newOptions: ParsedOptions = testOptions(originalOptions);

        // Load new options into the slider state
        updateAble.forEach(function (name: OptionKey) {
            if (optionsToUpdate[name] !== undefined) {
                (options[name] as any) = newOptions[name];
            }
        });

        scope_Spectrum = newOptions.spectrum;

        // Limit, margin and padding depend on the spectrum but are stored outside of it. (#677)
        options.margin = newOptions.margin;
        options.limit = newOptions.limit;
        options.padding = newOptions.padding;

        // Update pips, removes existing.
        if (options.pips) {
            pips(options.pips);
        } else {
            removePips();
        }

        // Update tooltips, removes existing.
        if (options.tooltips) {
            tooltips();
        } else {
            removeTooltips();
        }

        // Invalidate the current positioning so valueSet forces an update.
        scope_Locations = [];

        valueSet(isSet(optionsToUpdate.start) ? optionsToUpdate.start : v, fireSetEvent);
    }

    // Initialization steps
    function setupSlider(): void {
        // Create the base element, initialize HTML and set classes.
        // Add handles and connect elements.
        scope_Base = addSlider(scope_Target);

        addElements(options.connect, scope_Base);

        // Attach user events.
        bindSliderEvents(options.events);

        // Use the public value method to set the start values.
        valueSet(options.start);

        if (options.pips) {
            pips(options.pips);
        }

        if (options.tooltips) {
            tooltips();
        }

        aria();
    }

    setupSlider();

    const scope_Self: API = {
        destroy: destroy,
        steps: getNextSteps,
        on: bindEvent,
        off: removeEvent,
        get: valueGet,
        set: valueSet,
        setHandle: valueSetHandle,
        reset: valueReset,
        // Exposed for unit testing, don't use this in your application.
        __moveHandles: function (upward: boolean, proposal: number, handleNumbers: number[]) {
            moveHandles(upward, proposal, scope_Locations, handleNumbers);
        },
        options: originalOptions, // Issue #600, #678
        updateOptions: updateOptions,
        target: scope_Target, // Issue #597
        removePips: removePips,
        removeTooltips: removeTooltips,
        getPositions: function () {
            return scope_Locations.slice();
        },
        getTooltips: function () {
            return scope_Tooltips;
        },
        getOrigins: function () {
            return scope_Handles;
        },
        pips: pips, // Issue #594
    } as API;

    return scope_Self;
}

// Run the standard initializer
function initialize(target: TargetElement, originalOptions: Options): API {
    if (!target || !target.nodeName) {
        throw new Error("noUiSlider: create requires a single element, got: " + target);
    }

    // Throw an error if the slider was already initialized.
    if (target.noUiSlider) {
        throw new Error("noUiSlider: Slider was already initialized.");
    }

    // Test the options and create the slider environment;
    const options: ParsedOptions = testOptions(originalOptions);
    const api: API = scope(target, options, originalOptions);

    target.noUiSlider = api;

    return api;
}

export { TargetElement as target };

export { initialize as create };

export { cssClasses };

export default {
    // Exposed for unit testing, don't use this in your application.
    __spectrum: Spectrum,
    // A reference to the default classes, allows global changes.
    // Use the cssClasses option for changes to one slider.
    cssClasses: cssClasses,
    create: initialize,
};
