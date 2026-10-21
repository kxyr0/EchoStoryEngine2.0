cbuffer Camera : register(b0) { row_major float4x4 viewProjection; };
Texture2D skin : register(t0);
SamplerState pixelSampler : register(s0);
struct VertexInput { float3 position : POSITION; float3 normal : NORMAL; float2 uv : TEXCOORD; };
struct PixelInput { float4 position : SV_POSITION; float3 normal : NORMAL; float2 uv : TEXCOORD; };
PixelInput VSMain(VertexInput input) { PixelInput o; o.position=mul(float4(input.position,1),viewProjection); o.normal=input.normal; o.uv=input.uv; return o; }
float4 PSMain(PixelInput input) : SV_TARGET { float4 color=skin.Sample(pixelSampler,input.uv); clip(color.a-0.01); float light=.65+.35*saturate(dot(normalize(input.normal),normalize(float3(-.4,.8,-.6)))); return float4(color.rgb*light,color.a); }
