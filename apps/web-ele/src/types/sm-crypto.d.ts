declare module 'sm-crypto' {
  export const sm4: {
    decrypt: (
      text: string,
      key: string,
      options?: {
        iv?: string;
        mode?: 'cbc' | 'ecb';
        output?: 'array' | 'string';
        padding?: 'none' | 'pkcs#5' | 'pkcs#7';
      },
    ) => string;
    encrypt: (
      text: string,
      key: string,
      options?: {
        iv?: string;
        mode?: 'cbc' | 'ecb';
        output?: 'array' | 'string';
        padding?: 'none' | 'pkcs#5' | 'pkcs#7';
      },
    ) => string;
  };
}
