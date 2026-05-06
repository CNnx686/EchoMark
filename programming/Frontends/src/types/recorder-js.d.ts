// 类型定义
declare module "recorder-js" {
  interface RecorderConfig {
    numChannels?: number;
    sampleRate?: number;
    nFrequencyBars?: number;
    onAnalysed?: (data: { data: number[]; lineTo: number }) => void;
  }

  interface RecorderResult {
    blob: Blob;
    buffer: Float32Array[];
    sampleRate?: number;
  }

  class Recorder {
    constructor(context: AudioContext, config?: RecorderConfig);
    init(stream: MediaStream): Promise<void>;
    start(): Promise<void>;
    stop(): Promise<RecorderResult>;
    updateAnalysers(): void;
    setOnAnalysed(handler: (data: { data: number[]; lineTo: number }) => void): void;
    destroy?(): void;
  }

  namespace Recorder {
    function download(blob: Blob, filename?: string): void;
  }

  export default Recorder;
}
