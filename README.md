(CLIP):
pip install transformers torch torchvision

(rembg):
pip install rembg onnxruntime pillow

(Weaviate):
docker pull semitechnologies/weaviate

docker run -d -p 8099:8080 --name weaviate semitechnologies/weaviate:latest

pip install weaviate-client

(Ollama):
docker run -d -v ollama:/root/.ollama -p 11435:11434 --name ollama ollama/ollama

docker exec -it ollama ollama pull exaone3.5:7.8b

(Python 패키지):
pip install weaviate-client

pip install transformers

pip install torch

pip install pillow

pip install rembg

백터값 추가 :

1. batch_indexing.py 있는 곳으로 cd 명령어써서 이동
   
2. python batch_indexing.py 명령어 실행
   IMAGE_FOLDER = r"C:\_dev5\projects_2v\script_image1\src\main\resources\static\images"
   이부분의 경로를 수정
