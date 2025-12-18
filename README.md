(CLIP):

```
pip install transformers torch torchvision
```
```
from transformers import CLIPProcessor, CLIPModel

model_name = "openai/clip-vit-base-patch32"

model = CLIPModel.from_pretrained(model_name)
processor = CLIPProcessor.from_pretrained(model_name)

print("CLIP ViT-B/32 loaded")
```

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
3. yes 누르면 시작 (yes나오는 화면까지 시간 좀 걸림)
<img width="455" height="377" alt="image" src="https://github.com/user-attachments/assets/1facb759-4763-43c5-b35c-51b2be73041e" />

