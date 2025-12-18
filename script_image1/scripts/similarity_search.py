"""
유사 이미지 검색 스크립트
Spring Boot에서 호출할 메인 스크립트

사용법:
python similarity_search.py --id 123 --top 10
"""

import argparse
import json
import sys
from weaviate_manager import WeaviateManager

def search_similar_images(product_id, top_n=10):
    """
    주어진 product_id와 유사한 이미지 검색
    
    Args:
        product_id: 검색할 상품 ID
        top_n: 반환할 유사 이미지 개수
    
    Returns:
        JSON 형식의 유사 이미지 리스트
    """
    manager = None
    
    try:
        # Weaviate 연결
        manager = WeaviateManager()
        
        # 1. product_id로 벡터 조회
        query_vector = manager.get_vector_by_id(product_id)
        
        if query_vector is None:
            return {
                'success': False,
                'error': f'Product ID {product_id} not found in database'
            }
        
        # 2. 유사도 검색 (자기 자신 포함 top_n+1개 가져오기)
        results = manager.search_similar(query_vector, limit=top_n + 1)
        
        # 3. 자기 자신 제외
        filtered_results = [
            r for r in results 
            if r['product_id'] != product_id
        ][:top_n]
        
        return {
            'success': True,
            'query_product_id': product_id,
            'total_results': len(filtered_results),
            'similar_images': filtered_results
        }
        
    except Exception as e:
        return {
            'success': False,
            'error': str(e)
        }
    
    finally:
        if manager:
            manager.close()


def main():
    parser = argparse.ArgumentParser(description='Search similar images by product ID')
    parser.add_argument('--id', type=str, required=True, help='Product ID to search')  # int -> str
    parser.add_argument('--top', type=int, default=10, help='Number of similar images to return')
    parser.add_argument('--pretty', action='store_true', help='Pretty print JSON output')
    
    args = parser.parse_args()
    
    # 유사 이미지 검색
    result = search_similar_images(args.id, args.top)
    
    # JSON 출력
    if args.pretty:
        print(json.dumps(result, indent=2, ensure_ascii=False))
    else:
        print(json.dumps(result, ensure_ascii=False))
    
    # 종료 코드 반환
    sys.exit(0 if result['success'] else 1)


if __name__ == "__main__":
    main()
