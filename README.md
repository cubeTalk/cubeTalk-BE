# cubeTalk-BE

실시간 토론 채팅 서비스

## 🔧 개발 기간
- 24.08.22 ~  

## 🧑‍💻 기술 스택

<div align="center">
   <span style="color: blue; font-size: 24px; font-weight: bold;">🌐 Front-End</span>
</div>
<br>
<div align="center">
  <img src="https://img.shields.io/badge/React-61DAFB?style=for-the-badge&amp;logo=React&amp;logoColor=black">  
  <img src="https://img.shields.io/badge/TypeScript-3178C6?style=for-the-badge&logo=typescript&logoColor=white"/>
  <img src="https://img.shields.io/badge/React Query-FF4154?style=for-the-badge&logo=ReactQuery&logoColor=white"/>
  <img src="https://img.shields.io/badge/Styled Components-DB7093?style=for-the-badge&logo=StyledComponent&logoColor=white"/>
  <img src="https://img.shields.io/badge/Tailwind-06B6D4?style=for-the-badge&logo=tailwindcss&logoColor=white"/>
  <img src="https://img.shields.io/badge/Zustand-0854C1?style=for-the-badge&logo=Zustand&logoColor=white"/>
</div>

<br>
<br>

<div align="center">
   <span style="color: blue; font-size: 24px; font-weight: bold;">🖥️ Back-End</span>
</div>
<br>
<br>
<div align="center">
  <img src="https://img.shields.io/badge/Java-007396?style=for-the-badge&logo=java&logoColor=white"/>
  <img src="https://img.shields.io/badge/Spring%20Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white"/>
  <img src="https://img.shields.io/badge/AWS-232F3E?style=for-the-badge&logo=amazon-aws&logoColor=white"/>
  <img src="https://img.shields.io/badge/ECS-FF9900?style=for-the-badge&logo=amazon-ecs&logoColor=white"/>
  <img src="https://img.shields.io/badge/MongoDB-47A248?style=for-the-badge&logo=mongodb&logoColor=white"/>
  <img src="https://img.shields.io/badge/RabbitMQ-FF6600?style=for-the-badge&logo=rabbitmq&logoColor=white"/>
  <img src="https://img.shields.io/badge/STOMP-000000?style=for-the-badge&logoColor=white"/>
</div>


## 👥 역할 분담

<div align="center">
<table width="100%">
  <tr>
    <td width="50%" valign="top">
      <h3 align="center">🌐 Front-End-김은식</h3>
      <li>Figma 디자인 및 API 구성</li> 
      <li>메인 화면 및 토론 페이지 구현(sliding, infinite scroll)</li> 
      <li>동시 채팅(전체 채팅, 팀채팅)과 토론 진행 및 소켓 연결 관리</li> 
    </td>
    <td width="50%" valign="top">
      <h3 align="center">🖥️ Back-End-이다빈</h3>
      <li>WebSocket과 STOMP를 활용한 실시간 채팅 기능을 구현</li> 
      <li>Docker Compose를 활용하여 클러스터 환경을 구축</li> 
      <li>RabbitMQ를 메시지 브로커로 활용하여 비동기 처리를 구현</li>     </td>
  </tr>
</table>
</div>

## 🪧 아키텍쳐

<div align="center">
  <img src="https://github.com/user-attachments/assets/d09bd141-4002-45a6-bbbe-8f95136ef48b" width="800" />
  
</div>

## ✍️ 시작 가이드
⚙️ server

실행
```
# dotenv 필요
./gradlew build
java -jar cubetalk-server.jar
```

## <img src="https://github.com/user-attachments/assets/9cf93d6b-292b-44d8-84ea-502c6ccf7887" alt="주요기능" width="35" align="center">  주요 기능

<div align="center">
  <h3>토론 종류 - 찬반토론, 자유토론</h3>
  <img src="https://github.com/user-attachments/assets/90ee6999-dc11-4e17-a449-77820f8f6491" alt="토론 종류 - 찬반토론, 자유토론" width="1000">
  <br>
  찬성 반대로 팀을 정해서 하는 찬반토론과 자유롭게 토론할 수 있는 자유토론 중에 선택하여 참가할 수 있어요 

  <h3>토론 생성 및 참가</h3>
  <img src="https://github.com/user-attachments/assets/f5a509fd-9dd8-4b6c-9966-fe4594cb502c" alt="토론 생성 및 참가" width="1000">
  <br>
  사용자들과 자유롭게 토론할 채팅방을 생성해서 참가 할 수 있어요

  <h3>토론 설정 변경</h3>
  <img src="https://github.com/user-attachments/assets/869f1cd7-a741-4993-af28-618968b254fa" alt="토론 설정 변경" width="1000">
  <br>
  토론의 설명, 시간 그리고 최대 유저수 설정을 변경하거나 팀을 변경할 수 있어요

  <h3>토론 진행 - 준비, 시작 및 안내메세지</h3>
  <img src="https://github.com/user-attachments/assets/" alt="진행 - 준비, 시작 및 안내메세지" width="250">
  <br>
  모든 사용자가 준비가 되면 토론을 시작하고 설정한 토론시간 동안 토론을 할 수 있어요

  <h3>토론 진행 - 타이머 및 투표</h3>
  <img src="https://github.com/user-attachments/assets/" alt="진행 - 타이머 및 투표" width="250">
  <br>
  각 토론의 설정에 따라 타이머가 진행되고 토론이 종료가 되면 마지막으로 투표를 할 수 있어요
</div>

