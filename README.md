도커 추가
//ollama
docker run -d -v ollama:/root/.ollama -p 11434:11434 --name ollama ollama/ollama
//ollama 내부에 한국어 모델 설치 명령어
docker exec -it ollama ollama pull exaone3.5:7.8b


//weaviate
docker pull semitechnologies/weaviate:1.35.0-rc.0-f8d55cf.arm64


//weaviate (백터 이미지 백터화)
Weaviate 도커 실행 중 (포트 8099)
패키지다운  pip install weaviate-client transformers torch pillow rembg
