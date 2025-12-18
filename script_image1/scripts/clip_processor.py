"""
CLIP 이미지 처리 모듈
- rembg로 배경 제거
- CLIP으로 이미지 벡터화
"""

import torch
from PIL import Image
from transformers import CLIPProcessor, CLIPModel
from rembg import remove
import io

class CLIPImageProcessor:
    def __init__(self, model_name="openai/clip-vit-base-patch32"):
        """CLIP 모델 초기화"""
        print(f"Loading CLIP model: {model_name}...")
        self.device = "cuda" if torch.cuda.is_available() else "cpu"
        self.model = CLIPModel.from_pretrained(model_name).to(self.device)
        self.processor = CLIPProcessor.from_pretrained(model_name)
        self.model.eval()
        print(f"Model loaded on {self.device}")
    
    def remove_background(self, image_path):
        """rembg로 배경 제거"""
        try:
            with open(image_path, 'rb') as f:
                input_data = f.read()
            
            output_data = remove(input_data)
            image = Image.open(io.BytesIO(output_data)).convert('RGB')
            return image
        except Exception as e:
            print(f"Background removal failed for {image_path}: {e}")
            # 실패시 원본 이미지 사용
            return Image.open(image_path).convert('RGB')
    
    def encode_image(self, image):
        """이미지를 CLIP 벡터로 변환"""
        inputs = self.processor(images=image, return_tensors="pt").to(self.device)
        
        with torch.no_grad():
            image_features = self.model.get_image_features(**inputs)
            # 정규화
            image_features = image_features / image_features.norm(dim=-1, keepdim=True)
        
        return image_features.cpu().numpy()[0].tolist()
    
    def process_image(self, image_path, remove_bg=True):
        """이미지 처리 전체 파이프라인"""
        try:
            # 배경 제거
            if remove_bg:
                image = self.remove_background(image_path)
            else:
                image = Image.open(image_path).convert('RGB')
            
            # 벡터화
            vector = self.encode_image(image)
            
            return {
                'success': True,
                'vector': vector,
                'dimension': len(vector)
            }
        except Exception as e:
            return {
                'success': False,
                'error': str(e)
            }
    
    def process_batch(self, image_paths, remove_bg=True, batch_size=16):
        """배치 처리"""
        results = []
        
        for i in range(0, len(image_paths), batch_size):
            batch = image_paths[i:i+batch_size]
            
            for path in batch:
                result = self.process_image(path, remove_bg)
                results.append({
                    'path': path,
                    **result
                })
                
                if result['success']:
                    print(f"✓ Processed: {path}")
                else:
                    print(f"✗ Failed: {path} - {result['error']}")
        
        return results


if __name__ == "__main__":
    # 테스트
    processor = CLIPImageProcessor()
    
    test_image = "C:\\_dev5\\국책\\수동크롤링\\images\\test.jpg"
    result = processor.process_image(test_image)
    
    if result['success']:
        print(f"Vector dimension: {result['dimension']}")
        print(f"First 5 values: {result['vector'][:5]}")
    else:
        print(f"Error: {result['error']}")
