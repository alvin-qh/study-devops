export interface Times {
    nowString: () => string;
}

export type RunWithCallback = () => void;

export declare function runWith(name: string, cb: RunWithCallback): any;