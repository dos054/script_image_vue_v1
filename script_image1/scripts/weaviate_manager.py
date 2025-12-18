"""
Weaviate 벡터 DB 관리 모듈
- 스키마 생성
- 벡터 저장
- 유사도 검색
"""

import weaviate
from weaviate.classes.config import Configure, Property, DataType
from weaviate.classes.query import Filter  # Filter import 추가
import os
from pathlib import Path

class WeaviateManager:
    def __init__(self, host="localhost", port=8099):
        """Weaviate 클라이언트 초기화"""
        url = f"http://{host}:{port}"
        print(f"Connecting to Weaviate at {url}...")
        self.client = weaviate.connect_to_local(host=host, port=port)
        print("Connected to Weaviate")
        self.collection_name = "ProductImage"
    
    def create_schema(self):
        """ProductImage 컬렉션 생성"""
        try:
            # 기존 컬렉션 삭제 (있으면)
            if self.client.collections.exists(self.collection_name):
                self.client.collections.delete(self.collection_name)
                print(f"Deleted existing collection: {self.collection_name}")
            
            # 새 컬렉션 생성
            self.client.collections.create(
                name=self.collection_name,
                vectorizer_config=Configure.Vectorizer.none(),  # 우리가 직접 벡터 제공
                properties=[
                    Property(name="product_id", data_type=DataType.TEXT),  # INT -> TEXT 변경
                    Property(name="image_path", data_type=DataType.TEXT),
                    Property(name="image_name", data_type=DataType.TEXT),
                ]
            )
            print(f"Created collection: {self.collection_name}")
            return True
        except Exception as e:
            print(f"Schema creation failed: {e}")
            return False
    
    def insert_image(self, product_id, image_path, vector):
        """단일 이미지 벡터 저장"""
        try:
            collection = self.client.collections.get(self.collection_name)
            
            image_name = os.path.basename(image_path)
            
            collection.data.insert(
                properties={
                    "product_id": product_id,
                    "image_path": image_path,
                    "image_name": image_name,
                },
                vector=vector
            )
            
            print(f"✓ Inserted: {image_name} (ID: {product_id})")
            return True
        except Exception as e:
            print(f"✗ Insert failed for {image_path}: {e}")
            return False
    
    def batch_insert(self, data_list):
        """배치로 여러 이미지 저장
        data_list: [{'product_id': 1, 'image_path': '...', 'vector': [...]}]
        """
        collection = self.client.collections.get(self.collection_name)
        
        success_count = 0
        fail_count = 0
        
        with collection.batch.dynamic() as batch:
            for data in data_list:
                try:
                    image_name = os.path.basename(data['image_path'])
                    
                    batch.add_object(
                        properties={
                            "product_id": data['product_id'],
                            "image_path": data['image_path'],
                            "image_name": image_name,
                        },
                        vector=data['vector']
                    )
                    success_count += 1
                except Exception as e:
                    print(f"✗ Batch insert failed: {e}")
                    fail_count += 1
        
        print(f"\nBatch insert completed: {success_count} success, {fail_count} failed")
        return success_count, fail_count
    
    def search_similar(self, query_vector, limit=10):
        """벡터 유사도 검색"""
        try:
            collection = self.client.collections.get(self.collection_name)
            
            response = collection.query.near_vector(
                near_vector=query_vector,
                limit=limit,
                return_metadata=['distance', 'certainty']
            )
            
            results = []
            for item in response.objects:
                distance = item.metadata.distance if hasattr(item.metadata, 'distance') else 0.5
                certainty = item.metadata.certainty if hasattr(item.metadata, 'certainty') else None
                
                # certainty가 있으면 사용, 없으면 distance로 계산
                if certainty is not None:
                    similarity = certainty
                else:
                    # distance는 0에 가까울수록 유사, 따라서 1-distance
                    similarity = max(0.0, min(1.0, 1 - distance))
                
                results.append({
                    'product_id': item.properties['product_id'],
                    'image_path': item.properties['image_path'],
                    'image_name': item.properties['image_name'],
                    'similarity': similarity,
                    'distance': distance  # 디버깅용
                })
            
            return results
        except Exception as e:
            print(f"Search failed: {e}")
            import traceback
            traceback.print_exc()
            return []
    
    def get_vector_by_id(self, product_id):
        """product_id로 벡터 조회"""
        try:
            collection = self.client.collections.get(self.collection_name)
            
            response = collection.query.fetch_objects(
                filters=Filter.by_property("product_id").equal(product_id),
                limit=1,
                include_vector=True
            )
            
            if response.objects:
                vector = response.objects[0].vector
                # Weaviate v4는 {'default': [...]} 형태로 반환
                if isinstance(vector, dict) and 'default' in vector:
                    return vector['default']
                return vector
            else:
                print(f"No vector found for product_id: {product_id}")
                return None
        except Exception as e:
            print(f"Get vector failed: {e}")
            return None
    
    def count_objects(self):
        """저장된 객체 수 확인"""
        try:
            collection = self.client.collections.get(self.collection_name)
            result = collection.aggregate.over_all(total_count=True)
            count = result.total_count
            print(f"Total objects in {self.collection_name}: {count}")
            return count
        except Exception as e:
            print(f"Count failed: {e}")
            return 0
    
    def close(self):
        """연결 종료"""
        self.client.close()
        print("Weaviate connection closed")


if __name__ == "__main__":
    # 테스트
    manager = WeaviateManager()
    
    # 스키마 생성
    manager.create_schema()
    
    # 테스트 데이터 삽입
    test_vector = [0.1] * 512  # CLIP 벡터 차원
    manager.insert_image(1, "test_image.jpg", test_vector)
    
    # 카운트 확인
    manager.count_objects()
    
    manager.close()
