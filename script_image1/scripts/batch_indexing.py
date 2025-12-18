"""
배치 인덱싱 스크립트
C:\_dev5\국책\수동크롤링\imagesv2 폴더의 모든 이미지를
CLIP으로 벡터화하여 Weaviate에 저장

실행: python batch_indexing.py
"""

import os
from pathlib import Path
from clip_processor import CLIPImageProcessor
from weaviate_manager import WeaviateManager

# 설정
IMAGE_FOLDER = r"C:\_dev5\국책\수동크롤링\imagesv2"
REMOVE_BACKGROUND = True  # 배경 제거 여부
BATCH_SIZE = 16  # 한번에 처리할 이미지 수

def get_image_files(folder_path):
    """폴더에서 모든 이미지 파일 경로 가져오기"""
    image_extensions = {'.jpg', '.jpeg', '.png', '.bmp', '.webp'}
    image_files = []
    
    folder = Path(folder_path)
    if not folder.exists():
        print(f"Error: Folder not found: {folder_path}")
        return []
    
    for file in folder.iterdir():
        if file.is_file() and file.suffix.lower() in image_extensions:
            image_files.append(str(file))
    
    return sorted(image_files)

def extract_product_id(image_path):
    """파일명에서 product_id 추출
    형식: 20798351_1.jpg -> 20798351_1.jpg (확장자 포함)
    """
    filename = os.path.basename(image_path)
    # 파일명 전체 반환 (확장자 포함)
    return filename

def main():
    print("=" * 60)
    print("Image Batch Indexing to Weaviate")
    print("=" * 60)
    
    # 1. 이미지 파일 목록 가져오기
    print(f"\n[1/5] Scanning images from: {IMAGE_FOLDER}")
    image_files = get_image_files(IMAGE_FOLDER)
    
    if not image_files:
        print("No image files found!")
        return
    
    print(f"Found {len(image_files)} images")
    
    # 2. CLIP 프로세서 초기화
    print("\n[2/5] Initializing CLIP processor...")
    clip_processor = CLIPImageProcessor()
    
    # 3. Weaviate 초기화
    print("\n[3/5] Connecting to Weaviate...")
    weaviate_manager = WeaviateManager()
    
    # 스키마 생성 (기존 데이터 삭제됨 주의!)
    print("\n[4/5] Creating schema (existing data will be deleted)...")
    response = input("Continue? (yes/no): ")
    if response.lower() != 'yes':
        print("Aborted.")
        return
    
    weaviate_manager.create_schema()
    
    # 4. 이미지 처리 및 저장
    print(f"\n[5/5] Processing and indexing {len(image_files)} images...")
    print(f"Background removal: {REMOVE_BACKGROUND}")
    print("-" * 60)
    
    data_to_insert = []
    success_count = 0
    fail_count = 0
    
    for idx, image_path in enumerate(image_files, 1):
        print(f"\n[{idx}/{len(image_files)}] Processing: {os.path.basename(image_path)}")
        
        # CLIP 벡터화
        result = clip_processor.process_image(image_path, remove_bg=REMOVE_BACKGROUND)
        
        if result['success']:
            product_id = extract_product_id(image_path)
            
            data_to_insert.append({
                'product_id': product_id,
                'image_path': image_path,
                'vector': result['vector']
            })
            
            success_count += 1
            print(f"  ✓ Vectorized (product_id: {product_id})")
            
            # 배치 크기만큼 모이면 저장
            if len(data_to_insert) >= BATCH_SIZE:
                print(f"\n  → Inserting batch of {len(data_to_insert)} images...")
                weaviate_manager.batch_insert(data_to_insert)
                data_to_insert = []
        else:
            fail_count += 1
            print(f"  ✗ Failed: {result['error']}")
    
    # 남은 데이터 저장
    if data_to_insert:
        print(f"\n  → Inserting final batch of {len(data_to_insert)} images...")
        weaviate_manager.batch_insert(data_to_insert)
    
    # 5. 결과 요약
    print("\n" + "=" * 60)
    print("Indexing Complete!")
    print("=" * 60)
    print(f"Total images: {len(image_files)}")
    print(f"Success: {success_count}")
    print(f"Failed: {fail_count}")
    
    # Weaviate에 저장된 객체 수 확인
    print("\n" + "-" * 60)
    weaviate_manager.count_objects()
    
    # 정리
    weaviate_manager.close()
    print("\nDone!")

if __name__ == "__main__":
    main()
